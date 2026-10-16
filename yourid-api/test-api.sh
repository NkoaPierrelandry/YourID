#!/usr/bin/env bash
# ==============================================================================
# Script de test manuel de l API YourID -- a lancer morceau par morceau
# (copier-coller bloc par bloc dans le terminal) ou en entier une fois familier.
#
# Prerequis :
#   - Le serveur tourne (mvn spring-boot:run) sur BASE_URL
#   - jq installe (sudo apt-get install jq) -- utilise pour extraire les tokens/ids
#     des reponses JSON automatiquement, sans quoi il faudra copier les valeurs a la main.
# ==============================================================================

set -e
BASE_URL="http://localhost:4000/api/v1"

echo "=============================================="
echo "1. HEALTH CHECK"
echo "=============================================="
curl -s "$BASE_URL/health" | jq
echo

echo "=============================================="
echo "2. AUTH -- INSCRIPTION (vendeur)"
echo "=============================================="
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "kazeshinikeinest@gmail.com",
    "password": "MotDePasse123!",
    "firstName": "Keinest",
    "lastName": "Ledoux",
    "username": "ledoux_seller"
  }')
echo "$REGISTER_RESPONSE" | jq
TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.data.accessToken')
USER_ID=$(echo "$REGISTER_RESPONSE" | jq -r '.data.id')
echo "TOKEN capture : $TOKEN"
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
  echo "ERREUR : inscription echouee (compte deja existant ? voir les logs du serveur)."
  echo "Si kazeshinikeinest@gmail.com existe deja, supprimez-la ou changez l email dans ce script."
  #exit 1
fi
echo

echo "=============================================="
echo "3. AUTH -- CONNEXION (re-teste le login avec le meme compte)"
echo "=============================================="
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email": "kazeshinikeinest@gmail.com", "password": "MotDePasse123!"}')
echo "$LOGIN_RESPONSE" | jq
echo

echo "=============================================="
echo "4. USERS -- PROFIL COURANT"
echo "=============================================="
curl -s "$BASE_URL/users/me" -H "Authorization: Bearer $TOKEN" | jq
echo

echo "=============================================="
echo "5. USERS -- MISE A JOUR DU PROFIL"
echo "=============================================="
curl -s -X PATCH "$BASE_URL/users/me" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bio": "Vendeuse d ebooks sur YourID"}' | jq
echo

echo "=============================================="
echo "6. STORES -- CREATION DE LA BOUTIQUE"
echo "=============================================="
STORE_RESPONSE=$(curl -s -X POST "$BASE_URL/stores" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "La Boutique d Alice",
    "description": "Ebooks et formations",
    "currency": "XOF",
    "country": "SN"
  }')
echo "$STORE_RESPONSE" | jq
STORE_ID=$(echo "$STORE_RESPONSE" | jq -r '.data.id')
STORE_SLUG=$(echo "$STORE_RESPONSE" | jq -r '.data.slug')
echo "STORE_ID capture : $STORE_ID"
echo

echo "=============================================="
echo "7. STORES -- MA BOUTIQUE"
echo "=============================================="
curl -s "$BASE_URL/stores/me" -H "Authorization: Bearer $TOKEN" | jq
echo

echo "=============================================="
echo "8. STORES -- PAGE PUBLIQUE (par slug, sans auth)"
echo "=============================================="
curl -s "$BASE_URL/stores/$STORE_SLUG" | jq
echo

echo "=============================================="
echo "9. STORES -- MISE A JOUR"
echo "=============================================="
curl -s -X PATCH "$BASE_URL/stores/me" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"primaryColor": "#FF6600"}' | jq
echo

echo "=============================================="
echo "10. CATEGORIES -- LISTE PUBLIQUE"
echo "=============================================="
curl -s "$BASE_URL/categories" | jq
echo "(Si vide : aucune categorie n a encore ete inseree en base -- voir note en fin de script)"
echo

echo "=============================================="
echo "11. PRODUCTS -- CREATION D UN PRODUIT"
echo "=============================================="
PRODUCT_RESPONSE=$(curl -s -X POST "$BASE_URL/products" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Guide complet du marketing digital",
    "description": "Un ebook de 100 pages sur le marketing digital en Afrique",
    "shortDesc": "Le guide reference du marketing digital",
    "type": "EBOOK",
    "price": 5000,
    "currency": "XOF",
    "contentUrl": "https://drive.google.com/exemple",
    "contentType": "GDRIVE"
  }')
echo "$PRODUCT_RESPONSE" | jq
PRODUCT_ID=$(echo "$PRODUCT_RESPONSE" | jq -r '.data.id')
PRODUCT_SLUG=$(echo "$PRODUCT_RESPONSE" | jq -r '.data.slug')
echo "PRODUCT_ID capture : $PRODUCT_ID"
echo

echo "=============================================="
echo "12. PRODUCTS -- PUBLICATION (DRAFT -> PUBLISHED)"
echo "=============================================="
echo "Un produit cree est en DRAFT par defaut -- il doit passer en PUBLISHED pour"
echo "apparaitre sur le marketplace et etre achetable."
curl -s -X PATCH "$BASE_URL/products/$PRODUCT_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "PUBLISHED"}' | jq
echo

echo "=============================================="
echo "13. PRODUCTS -- MES PRODUITS (pagine)"
echo "=============================================="
curl -s "$BASE_URL/products/me?page=0&size=10" -H "Authorization: Bearer $TOKEN" | jq
echo

echo "=============================================="
echo "14. PRODUCTS -- FICHE PUBLIQUE (par slug, sans auth)"
echo "=============================================="
curl -s "$BASE_URL/products/$PRODUCT_SLUG" | jq
echo

echo "=============================================="
echo "15. MARKETPLACE -- PARCOURS PUBLIC"
echo "=============================================="
echo "-- Tous les produits publies --"
curl -s "$BASE_URL/marketplace/products" | jq
echo "-- Produits mis en avant (isFeatured, aucun pour l instant) --"
curl -s "$BASE_URL/marketplace/featured" | jq
echo

echo "=============================================="
echo "16. FILES -- UPLOAD D IMAGE (necessite un fichier reel)"
echo "=============================================="
echo "Remplacez /chemin/vers/image.jpg par un vrai fichier avant de lancer :"
echo 'curl -s -X POST "'"$BASE_URL"'/files/upload" \'
echo '  -H "Authorization: Bearer '"$TOKEN"'" \'
echo '  -F "file=@/chemin/vers/image.jpg" | jq'
echo

echo "=============================================="
echo "17. ORDERS -- CHECKOUT (achat, PUBLIC, sans auth)"
echo "=============================================="
ORDER_RESPONSE=$(curl -s -X POST "$BASE_URL/orders/checkout/$STORE_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail": "linedevils271@gmail.com",
    "customerFirstName": "Moussa",
    "customerLastName": "Fall",
    "items": [
      {"productId": "'"$PRODUCT_ID"'", "quantity": 1}
    ]
  }')
echo "$ORDER_RESPONSE" | jq
ORDER_NUMBER=$(echo "$ORDER_RESPONSE" | jq -r '.data.orderNumber')
echo "ORDER_NUMBER capture : $ORDER_NUMBER"
echo

echo "=============================================="
echo "18. ORDERS -- SUIVI DE COMMANDE (PUBLIC -- contentUrl doit etre absent, commande PENDING)"
echo "=============================================="
curl -s "$BASE_URL/orders/$ORDER_NUMBER" | jq
echo

echo "=============================================="
echo "19. ORDERS -- CONFIRMATION DE PAIEMENT (simule un webhook -- PAS SECURISE, voir notes)"
echo "=============================================="
curl -s -X POST "$BASE_URL/orders/$ORDER_NUMBER/complete" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "orange_money",
    "paymentRef": "REF-TEST-001",
    "provider": "orange_money",
    "providerRef": "PROVIDER-REF-001"
  }' | jq
echo

echo "=============================================="
echo "20. ORDERS -- SUIVI APRES PAIEMENT (contentUrl doit maintenant apparaitre)"
echo "=============================================="
curl -s "$BASE_URL/orders/$ORDER_NUMBER" | jq
echo

echo "=============================================="
echo "21. ORDERS -- MES COMMANDES (vendeur, pagine)"
echo "=============================================="
curl -s "$BASE_URL/orders/me?page=0&size=10" -H "Authorization: Bearer $TOKEN" | jq
echo

echo "=============================================="
echo "22. REVIEWS -- CREATION D UN AVIS (PUBLIC)"
echo "=============================================="
curl -s -X POST "$BASE_URL/reviews" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "'"$PRODUCT_ID"'",
    "rating": 5,
    "title": "Excellent guide",
    "comment": "Tres complet, je recommande !"
  }' | jq
echo

echo "=============================================="
echo "23. REVIEWS -- LISTE POUR UN PRODUIT (PUBLIC)"
echo "=============================================="
curl -s "$BASE_URL/reviews/product/$PRODUCT_ID" | jq
echo

echo "=============================================="
echo "24. NOTIFICATIONS -- LISTE (le vendeur doit avoir recu 'Nouvelle vente !')"
echo "=============================================="
curl -s "$BASE_URL/notifications" -H "Authorization: Bearer $TOKEN" | jq
echo

echo "=============================================="
echo "25. NOTIFICATIONS -- COMPTEUR NON LUES"
echo "=============================================="
curl -s "$BASE_URL/notifications/unread-count" -H "Authorization: Bearer $TOKEN" | jq
echo

echo "=============================================="
echo "26. ANALYTICS -- TRACKING D EVENEMENT (PUBLIC, anonyme)"
echo "=============================================="
curl -s -X POST "$BASE_URL/analytics/track" \
  -H "Content-Type: application/json" \
  -d '{
    "event": "product_view",
    "storeId": "'"$STORE_ID"'",
    "productId": "'"$PRODUCT_ID"'",
    "properties": "{\"page\":\"/p/'"$PRODUCT_SLUG"'\"}"
  }' | jq
echo

echo "=============================================="
echo "27. ANALYTICS -- TABLEAU DE BORD VENDEUR"
echo "=============================================="
curl -s "$BASE_URL/analytics/dashboard" -H "Authorization: Bearer $TOKEN" | jq
echo

echo "=============================================="
echo "28. WITHDRAWALS -- DEMANDE DE RETRAIT"
echo "=============================================="
echo "Le solde de la boutique doit etre > 0 (la commande a du etre completee a l etape 19)."
WITHDRAWAL_RESPONSE=$(curl -s -X POST "$BASE_URL/withdrawals" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1000,
    "method": "orange_money",
    "accountInfo": "{\"phone\":\"+221771234567\"}",
    "notes": "Premier retrait test"
  }')
echo "$WITHDRAWAL_RESPONSE" | jq
WITHDRAWAL_ID=$(echo "$WITHDRAWAL_RESPONSE" | jq -r '.data.id')
echo "WITHDRAWAL_ID capture : $WITHDRAWAL_ID"
echo

echo "=============================================="
echo "29. WITHDRAWALS -- MES RETRAITS"
echo "=============================================="
curl -s "$BASE_URL/withdrawals/me" -H "Authorization: Bearer $TOKEN" | jq
echo

echo "=============================================="
echo "30. WITHDRAWALS -- ROUTES ADMIN (necessitent un compte role ADMIN)"
echo "=============================================="
echo "Alice a le role SELLER par defaut -- ces appels vont echouer en 403 tant que"
echo "vous n avez pas un compte ADMIN. Pour en creer un, executez en base :"
echo "  UPDATE users SET role = 'ADMIN' WHERE email = 'kazeshinikeinest@gmail.com';"
echo "puis refaites un login pour obtenir un nouveau token avec le role ADMIN dans le JWT."
echo
echo "curl -s -X PATCH \"$BASE_URL/withdrawals/$WITHDRAWAL_ID/approve\" -H \"Authorization: Bearer \$ADMIN_TOKEN\" | jq"
echo "curl -s -X PATCH \"$BASE_URL/withdrawals/$WITHDRAWAL_ID/pay\" -H \"Authorization: Bearer \$ADMIN_TOKEN\" | jq"
echo

echo "=============================================="
echo "31. AI -- GENERATION DE CONTENU (necessite OPENAI_API_KEY configuree)"
echo "=============================================="
curl -s -X POST "$BASE_URL/ai/generate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "product_description",
    "prompt": "Ecris une description accrocheuse pour un ebook sur le marketing digital en Afrique"
  }' | jq
echo

echo "=============================================="
echo "32. PRODUCTS -- SUPPRESSION (nettoyage)"
echo "=============================================="
echo "Decommentez pour supprimer le produit de test :"
echo "curl -s -X DELETE \"$BASE_URL/products/$PRODUCT_ID\" -H \"Authorization: Bearer \$TOKEN\" | jq"
echo

echo "=============================================="
echo "TERMINE. Variables utiles capturees pendant ce script :"
echo "  TOKEN=$TOKEN"
echo "  USER_ID=$USER_ID"
echo "  STORE_ID=$STORE_ID"
echo "  PRODUCT_ID=$PRODUCT_ID"
echo "  ORDER_NUMBER=$ORDER_NUMBER"
echo "  WITHDRAWAL_ID=$WITHDRAWAL_ID"
echo "=============================================="