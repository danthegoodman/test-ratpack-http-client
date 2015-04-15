import ratpack.exec.Execution
import ratpack.func.Action
import ratpack.groovy.Groovy
import ratpack.http.client.HttpClient

import java.util.concurrent.atomic.AtomicBoolean
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
            println "/hello: Making ${n} requests"

            promise { fulfiller ->
                def counter = new AtomicInteger(n)
                Action<Execution> onComplete = { e ->
                    if (counter.decrementAndGet() == 0) {
                        fulfiller.success(null);
                    }
                };

                def error = new AtomicBoolean();
                Action<Throwable> onError = { t ->
                    if (error.compareAndSet(false, true)) {
                        fulfiller.error(t);
                    }
                };

                for(def i = 0; i < n; i++) {
                    context.exec()
                            .onError(onError)
                            .onComplete(onComplete)
                            .start({
                                http.get(new URI("http://localhost:5051/block")).then{}
                             })
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


