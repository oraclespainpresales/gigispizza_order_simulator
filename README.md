# Helidon Quickstart MP Example

This example implements a simple Hello World REST service using MicroProfile.

## Build and run

With JDK8+
```bash
mvn package
java -jar target/helidon-quickstart-mp.jar
```

## Exercise the application

```
curl -X GET http://localhost:8080/greet
{"message":"Hello World!"}

curl -X GET http://localhost:8080/greet/Joe
{"message":"Hello Joe!"}

curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Hola"}' http://localhost:8080/greet/greeting

curl -X GET http://localhost:8080/greet/Jose
{"message":"Hola Jose!"}
```

## Try health and metrics

```
curl -s -X GET http://localhost:8080/health
{"outcome":"UP",...
. . .

# Prometheus Format
curl -s -X GET http://localhost:8080/metrics
# TYPE base:gc_g1_young_generation_count gauge
. . .

# JSON Format
curl -H 'Accept: application/json' -X GET http://localhost:8080/metrics
{"base":...
. . .

```

## Build the Docker Image

```
docker build -t helidon-quickstart-mp .
```

## Start the application with Docker

```
docker run --rm -p 8080:8080 helidon-quickstart-mp:latest
```

Exercise the application as described above

## Deploy the application to Kubernetes

```
kubectl cluster-info                         # Verify which cluster
kubectl get pods                             # Verify connectivity to cluster
kubectl create -f app.yaml               # Deploy application
kubectl get service helidon-quickstart-mp  # Verify deployed service
```

To create a simulator JSON config file for postman or similar
minthreads and maxthreads are optional and default value is 20.
To simulate with microservices:
```json
{
    "sim-config" : {        
        "num-orders" : 100,  
        "pizza-status" : "PIZZA PAID",
        "min-threads":"10",
        "max-threads":"10",
        "microservice": {
            "url" : "https://madrid-gigispizza.wedoteam.io",
            "connection-timeout" : 500000,
            "response-timeout" : 500000
        }
    }
}
```
To simulate with Database:
```json
{
    "sim-config" : {        
        "num-orders" : 100,  
        "pizza-status" : "PIZZA PAID",
        "min-threads":"10",
        "max-threads":"10",
        "database": {
            "date-format" : "dd/MM/yyyy HH:mm:ss",
            "date-ini" : "13/06/2020 10:45:34",
            "connection-string" : "jdbc:oracle:thin:@//cdb.madrid-gigispizza.wedoteam.io:1521/dodbhp_pdb1.sub03010825490.devopsvcn.oraclevcn.com",
            "user" : "microservice",
            "password" : "AAZZ__welcomedevops123"
        }
    }
}
```