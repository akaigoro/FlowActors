In the article [Java 9 Reactive Streams][1] an attempt was made to implement 
[Java9 Flow interfaces][2]. However, that implementation was not at all asynchronous:
all the work is done in the the methods _OnNext()_ of Subscriber and Processor, that is, 
using the caller's stack. In fact,  that implementation provides just 
a set of ordinary classes which work synchronously, without any parallelism.

The implementation proposed in this article - [FlowActors][4] - is fully parallel and asynchronous.
All Publishers, Subscriber and Processors are independent activities, executing on a thread pool, 
and interacting without blocking or exploiting of the caller's stack.

Subscriber implementation can be considered as a typical Actor
aas described in the Carl Hewitt's model. 
For Publisher, The Hewitt model was slightly modified: 
the input of the Publisher actor consists not of the messages,
but of permissions, send from Subscribers via Flow.Subscription#requst(n) method.
We can consider the Publisher's input point as an asynchronous semaphore.
For Processor, wich is a combination of Publisher and Subscriber, the Actor model was extended further, allowing an Actor to have more than one input ports.
This kind of actors are called dataflow actors and first were developed to model hardware processors,
and later applied to software development, see, for example, [Dataflow Process Networks][6].

The library is very small (less than 600 lines of code), but passes the [TCK-Flow test suite][3]
The code was minimized in order to let reader programmer to use it as a starting point for further development.
As a result, it has some important restrictions: Publisher can serve only one Subscriber at a time;
Subscriber has room for only one incoming message. 
Those who prefer to use fully featured implementation, can look at 
more developed (but still compact) [Dataflow For Java] [5] framework.

[1]: https://www.baeldung.com/java-9-reactive-streams
[2]: https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/Flow.html
[3]: https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck-flow
[4]: https://github.com/akaigoro/FlowActors
[5]: https://github.com/akaigoro/df4j
[6]: https://pdfs.semanticscholar.org/2dfa/fb6ea86ac739b17641d4c4e51cc17d31a56f.pdf