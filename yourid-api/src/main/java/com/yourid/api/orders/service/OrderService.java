package com.yourid.api.orders.service;

import com.yourid.api.common.exception.BadRequestException;
import com.yourid.api.common.exception.ResourceNotFoundException;
import com.yourid.api.customers.entity.Customer;
import com.yourid.api.customers.repository.CustomerRepository;
import com.yourid.api.emails.service.EmailService;
import com.yourid.api.notifications.service.NotificationService;
import com.yourid.api.orders.dto.*;
import com.yourid.api.orders.entity.Order;
import com.yourid.api.orders.entity.OrderItem;
import com.yourid.api.orders.enums.OrderStatus;
import com.yourid.api.orders.repository.OrderRepository;
import com.yourid.api.payments.service.PaymentService;
import com.yourid.api.products.entity.Product;
import com.yourid.api.products.enums.ProductStatus;
import com.yourid.api.products.repository.ProductRepository;
import com.yourid.api.products.service.ProductService;
import com.yourid.api.stores.entity.Store;
import com.yourid.api.stores.repository.StoreRepository;
import com.yourid.api.stores.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final StoreService storeService;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ProductService productService;

    @Value("${app.platform.fee-percentage}")
    private BigDecimal feePercentage;

    // Checkout public (pas d authentification -- un acheteur n a pas forcement de compte).
    // Tous les produits d une commande doivent appartenir a la meme boutique.
    @Transactional
    public OrderResponse checkout(UUID storeId, CreateOrderRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique introuvable."));

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + itemRequest.getProductId()));

            if (!product.getStore().getId().equals(storeId)) {
                throw new BadRequestException("Tous les produits doivent appartenir a la meme boutique.");
            }
            if (product.getStatus() != ProductStatus.PUBLISHED) {
                throw new BadRequestException("Le produit \"" + product.getTitle() + "\" n est pas disponible a la vente.");
            }

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            items.add(OrderItem.builder()
                    .product(product)
                    .title(product.getTitle())
                    .price(product.getPrice())
                    .quantity(itemRequest.getQuantity())
                    .build());
        }

        BigDecimal platformFee = subtotal.multiply(feePercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal sellerRevenue = subtotal.subtract(platformFee);

        Customer customer = customerRepository.findByStoreIdAndEmail(storeId, request.getCustomerEmail())
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .store(store)
                        .email(request.getCustomerEmail())
                        .firstName(request.getCustomerFirstName())
                        .lastName(request.getCustomerLastName())
                        .build()));

        Order order = Order.builder()
                .store(store)
                .customer(customer)
                .orderNumber(generateOrderNumber())
                .subtotal(subtotal)
                .platformFee(platformFee)
                .sellerRevenue(sellerRevenue)
                .total(subtotal)
                .currency(store.getCurrency())
                .customerEmail(request.getCustomerEmail())
                .customerName(request.getCustomerFirstName() + " " + request.getCustomerLastName())
                .build();

        for (OrderItem item : items) {
            item.setOrder(order);
        }
        order.setItems(items);

        order = orderRepository.save(order);
        paymentService.createPendingTransaction(order, "manual");

        return toResponse(order, false);
    }

    /**
     * Simule la confirmation de paiement (webhook du fournisseur).
     * ATTENTION : en production, cette route doit imperativement verifier la signature
     * du webhook envoyee par le fournisseur de paiement -- ici, aucune verification
     * n est faite, a corriger avant mise en ligne reelle.
     */
    @Transactional
    public OrderResponse completeOrder(String orderNumber, CompleteOrderRequest request) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderNumber));

        if (order.isCompleted()) {
            throw new BadRequestException("Cette commande a deja ete completee.");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(Instant.now());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentRef(request.getPaymentRef());
        order = orderRepository.save(order);

        paymentService.markCompleted(order, request.getProviderRef());

        // Mise a jour des statistiques boutique
        Store store = order.getStore();
        store.setTotalRevenue(store.getTotalRevenue().add(order.getSellerRevenue()));
        store.setBalance(store.getBalance().add(order.getSellerRevenue()));
        int totalQuantity = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        store.setTotalSales(store.getTotalSales() + totalQuantity);
        storeRepository.save(store);

        // Mise a jour des statistiques par produit
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setTotalSales(product.getTotalSales() + item.getQuantity());
            BigDecimal lineRevenue = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            product.setTotalRevenue(product.getTotalRevenue().add(lineRevenue));
            productRepository.save(product);
        }

        // Mise a jour du client
        Customer customer = order.getCustomer();
        customer.setTotalSpent(customer.getTotalSpent().add(order.getTotal()));
        customer.setOrderCount(customer.getOrderCount() + 1);
        customerRepository.save(customer);

        // Email de confirmation a l acheteur + notification pour le vendeur
        emailService.sendOrderConfirmation(order.getCustomerEmail(), order.getCustomerName(), store.getName(), order.getOrderNumber());
        notificationService.notify(
                store.getUser(),
                "new_sale",
                "Nouvelle vente !",
                "Vous avez recu une nouvelle commande (" + order.getOrderNumber() + ") pour un montant de " + order.getSellerRevenue() + " " + order.getCurrency() + "."
        );

        return toResponse(order, true);
    }

    @Transactional(readOnly = true)
    public OrderResponse getByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderNumber));
        return toResponse(order, order.isCompleted());
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listMine(UUID userId, Pageable pageable) {
        Store store = storeService.findByUserId(userId);
        return orderRepository.findByStoreId(store.getId(), pageable).map(o -> toResponse(o, o.isCompleted()));
    }

    private String generateOrderNumber() {
        String number;
        do {
            number = "ORD-" + Instant.now().toEpochMilli() + "-" + (int) (Math.random() * 1000);
        } while (orderRepository.existsByOrderNumber(number));
        return number;
    }

    // revealContent : le contentUrl n est renvoye que si la commande est COMPLETED,
    // pour empecher un acheteur d acceder au contenu avant paiement confirme.
    private OrderResponse toResponse(Order order, boolean revealContent) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .title(item.getTitle())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .contentUrl(revealContent ? item.getProduct().getContentUrl() : null)
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .platformFee(order.getPlatformFee())
                .sellerRevenue(order.getSellerRevenue())
                .total(order.getTotal())
                .currency(order.getCurrency())
                .customerEmail(order.getCustomerEmail())
                .customerName(order.getCustomerName())
                .items(items)
                .completedAt(order.getCompletedAt())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public ProductService.ContentDownload downloadForBuyer(String orderNumber, UUID productId) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable."));

        if (!order.isCompleted()) {
            throw new BadRequestException("Cette commande n est pas encore payee.");
        }

        boolean belongsToOrder = order.getItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(productId));
        if (!belongsToOrder) {
            throw new ResourceNotFoundException("Ce produit ne fait pas partie de cette commande.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable."));

        return productService.openContent(product);
    }
}
