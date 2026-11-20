# Product API

Une petite API pour gérer des produits, des catégories et des commandes. Elle est faite avec Spring Boot et Java 21, stocke ses données dans MySQL, et protège ses routes avec des tokens JWT.

## Lancer le projet

Il suffit d'avoir Docker Desktop.

```bash
docker compose up -d --build
```

L'API répond sur http://localhost:8080. La base MySQL est sur `localhost:3307` (base `productdb`, utilisateur `root`, mot de passe `root`).

Au démarrage, deux catégories et trois produits sont ajoutés pour avoir de quoi tester.

Pour tout arrêter :

```bash
docker compose down
```

Et pour repartir d'une base vide, ajoutez `-v` :

```bash
docker compose down -v
```

## Se connecter

Presque toutes les routes demandent d'être connecté. On commence par créer un compte :

```bash
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"email\":\"admin@test.local\",\"password\":\"secret123\",\"role\":\"ADMIN\"}"
```

Puis on se connecte pour recevoir un token :

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"secret123\"}"
```

On envoie ensuite ce token avec chaque requête :

```bash
curl http://localhost:8080/api/products -H "Authorization: Bearer <token>"
```

Un token est valable 15 minutes. Pour rester connecté, on appelle `POST /api/auth/refresh` avec le token actuel pendant ses 10 dernières minutes : on reçoit un nouveau token. Au bout de 7 jours, il faut se reconnecter.

Cette durée courte sert aussi de protection : si un token est volé, il ne sert que quelques minutes.

## Qui peut faire quoi

Il y a trois rôles. Sans précision à l'inscription, on est `USER`.

| | USER | MANAGER | ADMIN |
|---|---|---|---|
| Voir les produits et les catégories | oui | oui | oui |
| Ajouter un produit | | oui | oui |
| Modifier ou supprimer un produit | | | oui |
| Gérer les catégories | | | oui |
| Passer une commande | oui | | |
| Voir les commandes | les siennes | toutes | toutes |
| Supprimer une commande | | | oui |

Sans token, l'API répond 401. Avec un rôle insuffisant, elle répond 403.

## Les routes

**Produits** : `/api/products`
- `GET /api/products` liste les produits, page par page (`?page=0&size=10&sort=id`)
- `GET /api/products/{id}` et `GET /api/products/search?name=...`
- `POST`, `PUT /{id}` et `DELETE /{id}` pour ajouter, modifier et supprimer

**Catégories** : `/api/categories`
- `GET /api/categories`, `GET /api/categories/{id}` et `GET /api/categories/{id}/products`
- `POST`, `PUT /{id}` et `DELETE /{id}`. Attention : supprimer une catégorie supprime aussi ses produits.

**Commandes** : `/api/orders`
- `GET /api/orders` et `GET /api/orders/{id}`
- `POST /api/orders` : la commande est enregistrée au nom de la personne connectée
- `DELETE /api/orders/{id}`

Exemple de commande :

```json
{ "items": [ { "productId": 1, "quantity": 2 } ] }
```

## Quelques règles

- Un produit a un nom unique et un prix supérieur à 0. Sa quantité va de 0 à 1000.
- Une catégorie a un nom.
- Une commande contient au moins un article.

Si une règle n'est pas respectée, l'API répond 400 avec un message qui explique ce qui ne va pas.

## Tester avec Postman

Importez `postman/Lab4-Auth.postman_collection.json`, puis lancez « Run collection ». Elle crée des comptes de test, se connecte avec chaque rôle et vérifie que chacun a bien les bons droits.
