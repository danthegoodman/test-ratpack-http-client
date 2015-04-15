import ratpack.groovy.Groovy
import ratpack.http.client.HttpClient

import java.util.concurrent.atomic.AtomicInteger

Groovy.ratpack {
    serverConfig {
        port 5051
    }

    handlers {
        handler 'hello', {
            def start = System.currentTimeMillis()

            def http = get(HttpClient)
            def n = request.queryParams['times'] as Integer ?: 5
            def counter = new AtomicInteger(n)

            println "/hello: Making ${n} requests"
            promise { fulfiller ->
                for(def i = 0; i < n; i++){
                    http.get(new URI("http://localhost:5051/block"))
                        .onError({ fulfiller.error(it)})
                        .then {
                            if(counter.decrementAndGet() <= 0){
                                fulfiller.success(null)
                            }
                        }
                }
            }.then {
                def msg = "made ${n} requests in ${System.currentTimeMillis() - start} ms"
                println "/hello: ${msg}"
                render msg
            }
        }

        handler 'block', {
            def start = System.currentTimeMillis()
            def time = request.queryParams['time'] as Integer ?: 500
            println "/block: Recieved wait request for $time ms"
            blocking {
                Thread.sleep(time)
            }.then{
                def msg = "done in ${System.currentTimeMillis() - start} ms"
                println "/block: ${msg}"
                render msg
            }
        }
    }

}


