![MacDown logo](http://imageshack.com/a/img924/4162/p5NF6P.png)

# Thoune Bot Framework

Thoune bot framework est un ensemble de projets réalisés dans le cadre d'un cours de **webservice**
Celui-ci s'articule autour de plusieurs composants, qui seront tous présentés dans ce readme. 

L'objectif était simple : créer un bot capable de répondre directement sur Facebook

Cependant, la GrimuTeam à décidé de s'attaquer à un projet de bot hautement modulable, pouvant être étendu très facilement, en utilisant l'unification des messages et en offrant une grande souplesse sur l'API principale.

## Architecture logicielle du projet

Le projet s'articule donc autour de cette API, qui sert en fait de **routeur/gateway** entre chacun des services. D'un côté, les connecteurs vers les réseaux sociaux (**Messenger** dans le cas précis, mais d'autres peuvent être développés facilement), de l'autre coté ce qu'on appellera des **intentions**, qui seront capables de répondre à un certain type de message.

Voici une présentation du flux d'un message dans Thoune

![flux](http://imageshack.com/a/img924/4108/BXYYIW.png)

### Le principe d'intentions

Thoune fonctionne avec Luis, l'intelligence artificielle de Microsoft (Cognitives services). C'est ce qui permet à Thoune de comprendre les messages, et ainsi de les retransmettre aux services qui seront capables de traiter ce type de message. 

Ce système d'intention permet simplement d'éclater la logique de traitement des différents types de messages entre plusieurs programmes, conçus pour ne répondre qu'à une seule problématique. 

####  Les avantages de ce système sont très nombreux : 

* **Simplifications** des programmes. Chacun s'occupe de son thème
* Adaptation du **langage de programmation** au thème choisi. Par exemple, NodeJS peut suffire pour répondre "Bonjour", mais Python peut être nécessaire pour traiter des messages plus complexes
* Réutilisation forte de services déjà créés. On peut **composer** son Thoune avec les projets de **la communauté**, il suffit des les connecter à votre API
* Transmission et traitement unifié de tous les types de messages (de toutes les plateformes)
* Pré-traitement du message effectué dans l'API par Luis, et utilisable facilement.  
* Aucune modification à faire dans l'API ni dans les connecteurs, votre service d'intention s'interconnecte automatiquement avec Thoune

Concrètement, un service d'intention écoute sur une queue RabbitMQ nommée avec le pattern : 
``` bot-intent-$INTENT```
et répond sur 
```bot-intent-api ```

l'API redirige le message grâce à un merge des intentions défini et reconnu dans Luis avec les intentions qui sont enregistrées dans la base de données de l'API. 

Ainsi, il vous suffit de créer une intention dans LUIS, et d'enregistrer son nom dans l'API via la route
``` POST /intent ``` pour que celle-ci soit prise en compte par l'API, les messages seront redirigés dans la queue

En cas d'intent non reconnu, le message est redirigé vers bot-intent-None
#### Projets de la communauté :

[Bot-intent-None : Permet de répondre quand Thoune ne comprend pas](https://github.com/haris44/bot-intent-none)

[Bot-intent-chucknorris : Funfact de Chucknorris](https://github.com/Netoun/bot-intent-chucknorris)

[Bot-intent-classement : Permet d'obtenir le classement de certaines ligues](https://github.com/Netoun/bot-intent-insulte)

[Bot-intent-insulte : Donne un peu de répartie à Thoune](https://github.com/Netoun/bot-intent-classement)

Il est très simple de créer un intent, basez vous simplement sur le projet bot-intent-None

#### Connecteurs :

Dans le cadre de ce projet, nous n'avons créé qu'un connecteur pour Messenger. Cependant, il est possible d'en ajouter (bien qu'une petite modification devra être apportée à l'API) 

[Connecteur Messenger](https://github.com/clusson/bot-messenger)

#### Frontend :

Également, nous avons créé un petit projet front-end en CycleJS afin de visualiser les messages, et poster les intents

[Frontend](https://github.com/NathanGrimaud/ProjetWebService-Front)


#### RabbitMock :

RabbitMock est une librairie de test pour les services d'intent. Vous trouverez des exemples de l'utilisation de celle-ci dans le service de bot-intent-None

```yarn add rabbitmock```

### RabbitMQ et le système de queueing

RabbitMQ à été retenu dans le cadre de ce projet pour sa capacité à faire du routage grâce au système de topic, ainsi qu'à garder les messages tant qu'ils ne sont pas traités.
En effet, si un de vos intents ne fonctionne plus, Thoune gardera le message en mémoire, et le redistribuera quand votre service sera de nouveau opérationnel. Ainsi, vos utilisateurs ne restent jamais sans réponse (simplement dans l'attente) ! 

### Utilisation de la base de données ElasticSearch

La base de données utilisée par Thoune est ElasticSearch (via la dépendance scala Elastic4S) 
La raison est simple : Elastic est une base de données documents supportant également le time-series. Cela nous permet ainsi d'utiliser qu'un seul type de base de données tout en profitant de tous les avantages d'une base de données Time-Series. Cela permet par exemple d'y installer un Grafana, et ainsi obtenir de très nombreuses informations autour du bot.


## Architecture système du projet

Ce système est totalement scalable, grâce aux choix architecturaux qui ont été faits. 
Ainsi, tout les services d'intent ou les connecteurs peuvent être dupliqués sur plusieurs serveurs (dans le cadre de notre production, nous utilisons PM2 pour permettre cela). L'API principale peut également, derrière un load balancer, être scalée sans problème. Enfin, Elastic et Rabbit offrent tous les deux la possibilité d'être clusterisé. Dans le cadre de la réalisation de notre projet, voici l'architecture système retenu :   

![sys](http://imageshack.com/a/img922/9051/zDrcpy.jpg)

Le serveur jaune étant celui qui héberge les intents. 

### Déploiement continu et Ansible

Dans le cadre de notre production, et afin de simplifier notre système de déploiement, nous utilisons Jenkins et Ansible. Ainsi, chaque projet peut être déployé sur un nouveau serveur sans administration réseau supplémentaire.

Si vous avez besoin de davantage d'informations, nous pouvons mettre les playbooks et les configurations à dispositions


## L'API 

Cette API sert donc de point central entre tous les services. C'est la seule à avoir accès à l'ElasticSearch, et à pouvoir être interrogé par le front-end. 

Je vous invite à consulter la docs d'API sur 

```GET /swagger.json```

Afin que l'API reste scalable, celle-ci n'embarque pas de [SwaggerUI](hhttp://petstore.swagger.io/). Mais il est tout à fait possible de lire le swagger.json ici : [SwaggerUI](hhttp://petstore.swagger.io/)

Tous nos projets sont configurables via les variables d'environnement. 
Voici un exemple de .env vide : 

```
RABBIT_ACTOR=
RABBIT_HOST=
RABBIT_PORT=
RABBIT_USER=
RABBIT_PASSWORD=

ELASTIC_HOST=
ELASTIC_PORT=

LUIS_URL=

RABBIT_QUEUE_API_MESSAGE=bot-api-message
RABBIT_BINDING_API_MESSAGE=bot.api.message
RABBIT_BINDING_FACEBOOK=bot.facebook
RABBIT_BINDING_API_USER=bot.api.user
RABBIT_QUEUE_API_USER=bot-api-user
RABBIT_QUEUE_FACEBOOK=bot-facebook
RABBIT_EXCHANGE=bot.topic
RABBIT_QUEUE_API_INTENT=bot-api-intent
RABBIT_BINDING_API_INTENT=bot.api.intent
```







