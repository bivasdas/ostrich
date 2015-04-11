# Performance Test Suite for Ostrich

## Objective

The point of this performance test is to be able to recreate Ostrich performance problems observed in 
 production, with the eventual aim of being able to fix those performance problems.

Anecdotally, services would run fine as long as the total number of requests-per-second to an Ostrich 
 service pool was low. As the requests per second to the Ostrich Service Pool increased, the performance
 of Ostrich would degrade, sometimes catastrophically.

This test has many parameters that can be adjusted

  * The requests-per-second to Ostrich
  * What work the "Ostrich client" does for each request
  * Ostrich "cache" parameters (How and when Ostrich will request new Client Objects for each Endpoint)

Note this test is specifically testing a "backend" component of Ostrich (the ServiceCache) that we've 
 identified as a problem. This does not test Ostrich as a whole

There should be a "black box" perf test for Ostrich in the future.   

## Defining Services, Caches and Pools

We have a fixed number of singleton services that performs cryptographic hash on a given String. A service 
factory provides a wrapper for those services and returned a wrapped service as and when requested by the 
 ServiceCache.

### Cache Test
A service cache is initialized with various parameters such as max number of service instances, idle time 
 before evicting a (wrapped) service instance, policy to  adhere upon cache is exhausted, etc. A detailed
 breakdown of available parameters are provided in the Parameters section.
 
### Pool test
A service pool with a service cache is initialized with specific numbers of servers. See parameters section.

## Running The Suite

To determine the performance of Ostrich under varying loads, this test suite takes various parameters. They 
 allow us to set the load on the cache, i.e. # of worker threads using the cache, the load on each thread,
 i.e. how much work each worker thread must do, etc. These help us to determine the overhead of ostrich 
 under nominal to mediocre to somewhat heavy loads. 

After determining and setting those desired values the suite will run the desired number of threads and
 load, and will monitor the health and performance of the cache.

## Parameters

### Standard help/usage message 

      -h,--help                   Show this help message!
      
### ServiceCache specific parameters

      -e,--exhaust-action <arg>   Exhaust action when cache is exhausted,
                                  acceptable values are WAIT|FAIL|GROW, default
                                  is WAIT
      -i,--idle-time <arg>        Idle time before service cache should take
                                  evict action, default is 10
      -m,--max-instances <arg>    Max instances per end point in service cache,
                                  default is 10
      -g,--new-cache              Run with new multi threaded cache, default is
                                  false

### ServicePool specific parameters
      
      -z,--zookeeper-port <arg>   zookeeper port to use, default is 2181
      -p,--starting-port <arg>    starting port to use for services, default is
                                  8000
      -n,--num-servers <arg>      number of services to instantiate, default is
                                  10

### Overall load tweaking parameters

      -t,--thread-size <arg>      # of workers threads to run, default is 100

      -w,--work-size <arg>        length of the string to generate randomly and
                                  crunch hash, default is 5120 (5kb)

### Chaos parameter to cause havoc on cache

      -c,--chaos-count <arg>      Number of chaos workers to use, default is 2

      -l,--chaos-interval <arg>   time (in seconds) to wait between chaos,
                                  default is 15

### Runtime parameters

      -T,--test-type <arg>        test type to run, POOL or CACHE, must be provided

      -r,--run-time <arg>         seconds to run before it kills worker running
                                  threads, default is 9223372036854775807
                                  (Long.MAX_VALUE)

      -v,--report-every <arg>     Reports the running statistics every #
                                  seconds

### Recommended JVM parameters

      -Xms2048m                   Allocate enough heap space such that it does not
                                  ran out of memory
                                  2GB of more is recommended 

      -Djava.compiler=NONE        Disable JIT compiler to keep things consistent
                                  This will avoid surprise optimization and as a
                                  result consequent performance gain


## Interpreting the results

The header summarises the applicable running parameters. All the Metrics gathered from the app are 
 displayed below that.

## Running Example

### Build the assembled dependency included jar

    mvn clean compile assembly:single

This creates a shaded jar called test-suite-<VERSION>-jar-with-dependencies.jar with all dependencies included.

### Run the suite

#### For the following configuration:

    - Heap size: 2GB (or more, based on how many threads you want to run), JIT disabled
    - threads: 25, work size: 12000Bytes
    - cache idle time: 10 seconds, cache max instance per service: 10, cache exhaust action: GROW
    - report every 5 second, run for 30 second 

#### The command would be:

    java -Djava.compiler=NONE -Xms2048m -jar test-suite-<VERSION>-jar-with-dependencies.jar \
    --thread-size 25 --work-size 12000 \
    --idle-time 10 --max-instances 10 --exhaust-action GROW \
    --report-every 5 --run-time 30

Or

    java -Djava.compiler=NONE -Xms2048m -jar test-suite-<VERSION>-jar-with-dependencies.jar -t 25 -w 12000 -i 10 -m 10 -e GROW-v 5 -r 30
