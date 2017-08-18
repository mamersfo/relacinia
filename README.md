## Relacinia

The frontend is based on [Relay Modern Hello World](https://github.com/apollographql/relay-modern-hello-world).

The backend is built using Clojuru, [Lacinia](https://github.com/walmartlabs/lacinia) (an implementation of the [GraphQL specification](https://facebook.github.io/graphql/)) and [Datomic Free](https://my.datomic.com/downloads/free).

### Run the GraphQL backend
```
lein ring server-headless 8080
```

### Run the frontend
```
yarn
yarn start
```

### Run the Relay Compiler
```
yarn run relay -- --watch
```

