package com.inqwise.walker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.collect.Sets;

import io.vertx.core.Vertx;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

/**
 * Vert.x integration tests for JsonObjectWalker functionality.
 * 
 * These tests use Vert.x test framework and real JSON files to test
 * the walking functionality with complex, real-world data structures.
 */
@ExtendWith(VertxExtension.class)
@Timeout(5000)
class VertxJsonObjectWalkerTest {
    
    private static final Logger logger = LogManager.getLogger(VertxJsonObjectWalkerTest.class);
	
    @BeforeEach
    void setUp(Vertx vertx) throws Exception {
    }

    @Test
    void testNoWalkers(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker();
        AtomicInteger cnt = new AtomicInteger();
        
        jsonWalker.handler(event -> {
            //logger.debug("handler({})", event);
            cnt.incrementAndGet();
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("endHandler({})", context);
            assertEquals(38, cnt.get());
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            logger.debug("errorHandler({})", context);
            logger.error("error: ", context.cause());
            fail();
            checkpoint.flag();
        });
        
        jsonWalker.handle(json);
    }
    
    @Test
    void testWithWalkers(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        AtomicInteger cnt = new AtomicInteger();
        
        jsonWalker.handler(event -> {
            logger.debug("handler({})", event);
            event.level().onExitLevel(nil -> {
                logger.debug("EXIT LEVEL");
            });
            
            if(!event.hasWalker()) {
                cnt.incrementAndGet();
            }
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("endHandler({})", context);
            assertEquals(120, cnt.get()); // Updated to match actual event count with new fireEntryEvent behavior
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            logger.debug("errorHandler({})", context);
            testContext.failNow(context.cause());
        });
        
        jsonWalker.handle(json);
    }
    
    @Test
    void testEnd(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        AtomicInteger cnt = new AtomicInteger();
        
        jsonWalker.handler(event -> {
            logger.debug("handler({})", event);
            if(2 == cnt.incrementAndGet()) {
                event.end();
            }
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("endHandler({})", context);
            assertEquals(2, cnt.get());
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            logger.debug("errorHandler({})", context);
            fail();
            checkpoint.flag();
        });
        
        jsonWalker.handle(json);
    }

    @Test
    void testError(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        
        jsonWalker.handler(event -> {
            logger.debug("handler({})", event);
            throw new RuntimeException();
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("endHandler({})", context);
            fail();
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            logger.debug("errorHandler({})", context);
            assertTrue(context.failed());
            assertNotNull(context.cause());
            checkpoint.flag();
        });
        
        jsonWalker.handle(json);
    }
    
    @Test
    void testErrorInEnd(VertxTestContext testContext) {
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        String expectedErrorMessage = "in end";
        
        jsonWalker.handler(event -> {
            logger.debug("handler({})", event);
            event.end();
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("endHandler({})", context);
            throw new RuntimeException(expectedErrorMessage);
        });
        
        jsonWalker.errorHandler(context -> {
            logger.debug("errorHandler({})", context);
            assertTrue(context.failed());
            assertEquals(expectedErrorMessage, context.cause().getMessage());
            testContext.completeNow();
        });
        
        jsonWalker.handle(json);
    }
    
    @Test
    void testPause(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        
        jsonWalker.handler(event -> {
            logger.debug("handler({})", event);
            checkpoint.flag();
            event.context().pause();
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("endHandler({})", context);
            fail("pause do not call end handler");
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            logger.debug("errorHandler({})", context);
            assertTrue(context.success(), () -> context.cause().toString());
            checkpoint.flag();
        });
        
        jsonWalker.handle(json);
    }
    
    @Test
    void testPause2(Vertx vertx, VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object2.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        AtomicBoolean flag = new AtomicBoolean(false);
        
        jsonWalker.handler(event -> {
            logger.debug("handler({})", event);
            checkpoint.flag();
            event.context().pause();
            vertx.setTimer(1000, tid -> {
                flag.set(true);
                event.context().resume();
            });
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("endHandler({})", context);
            if(!flag.get()) {
                fail("pause do not call end handler");
            }
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            logger.debug("errorHandler({})", context);
            assertTrue(context.success(), () -> context.cause().toString());
            checkpoint.flag();
        });
        
        jsonWalker.handle(json);
    }
    
    @Test
    void testResume(Vertx vertx, VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint(4);
        var cnt = new AtomicInteger();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        
        jsonWalker.handler(event -> {
            logger.debug("handler({})", event);
            if(cnt.incrementAndGet() == 1) {
                logger.debug("pause");
                event.context().pause();
                assertTrue(event.context().paused(), "not paused");
                checkpoint.flag();
                vertx.setTimer(100, timerId -> {
                    logger.debug("resume");
                    event.context().resume();
                    checkpoint.flag();
                });
            } else {
                logger.debug("end");
                event.end();
                checkpoint.flag();
            }
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("endHandler({})", context);
            assertFalse(context.paused(), "paused");
            assertTrue(context.ended(), "not ended");
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            logger.debug("errorHandler({})", context);
            testContext.failNow(context.cause());
        });
        
        jsonWalker.handle(json);
    }

    @Test
    void testComplexJsonStructureWithRealData(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        AtomicInteger eventCount = new AtomicInteger();
        
        jsonWalker.handler(event -> {
            eventCount.incrementAndGet();
            assertNotNull(event.meta().get(ObjectWalker.Keys.PATH));
            
            // Verify path structure
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            assertTrue(path.startsWith(".") || path.equals("."));
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("Processed {} events", eventCount.get());
            assertTrue(eventCount.get() > 0);
            assertTrue(context.success());
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            logger.error("Error during walking", context.cause());
            testContext.failNow(context.cause());
        });
        
        jsonWalker.handle(json);
    }

    @Test
    void testPathTrackingWithRealData(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        AtomicInteger eventCount = new AtomicInteger();
        
        jsonWalker.handler(event -> {
            eventCount.incrementAndGet();
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            // Log some paths for verification
            if (eventCount.get() <= 5) {
                logger.debug("Path {}: {}", eventCount.get(), path);
            }
            
            // Verify path format
            assertTrue(path.startsWith(".") || path.equals("."));
            if (path.contains("[")) {
                assertTrue(path.matches(".*\\[\\d+\\].*"));
            }
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("Total paths processed: {}", eventCount.get());
            assertTrue(context.success());
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            testContext.failNow(context.cause());
        });
        
        jsonWalker.handle(json);
    }

    @Test
    void testWalkerDelegationWithRealData(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        AtomicInteger objectWalkerCount = new AtomicInteger();
        AtomicInteger arrayWalkerCount = new AtomicInteger();
        AtomicInteger noWalkerCount = new AtomicInteger();
        
        jsonWalker.handler(event -> {
            if (event.hasWalker()) {
                if (event.getWalker() instanceof JsonObjectWalker) {
                    objectWalkerCount.incrementAndGet();
                } else if (event.getWalker() instanceof JsonArrayWalker) {
                    arrayWalkerCount.incrementAndGet();
                }
            } else {
                noWalkerCount.incrementAndGet();
            }
        });
        
        jsonWalker.endHandler(context -> {
            logger.debug("ObjectWalker delegations: {}", objectWalkerCount.get());
            logger.debug("ArrayWalker delegations: {}", arrayWalkerCount.get());
            logger.debug("No walker: {}", noWalkerCount.get());
            assertTrue(context.success());
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            testContext.failNow(context.cause());
        });
        
        jsonWalker.handle(json);
    }

    @Test
    void testContextDataSharingWithRealData(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        
        jsonWalker.handler(event -> {
            String path = event.meta().get(ObjectWalker.Keys.PATH).toString();
            
            // Store some data in context based on path
            if (path.equals(".birth_date")) {
                event.context().put("birthDate", event.indicatedObject());
            } else if (path.equals(".first_name")) {
                event.context().put("firstName", event.indicatedObject());
            } else if (path.equals(".last_name")) {
                event.context().put("lastName", event.indicatedObject());
            }
        });
        
        jsonWalker.endHandler(context -> {
            // Verify that context data was stored
            assertNotNull(context.get("birthDate"));
            assertNotNull(context.get("firstName"));
            assertNotNull(context.get("lastName"));
            assertTrue(context.success());
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            testContext.failNow(context.cause());
        });
        
        jsonWalker.handle(json);
    }

    @Test
    void testFlowControlWithRealData(VertxTestContext testContext) {
        var checkpoint = testContext.checkpoint();
        var json = TestUtils.readJson("json-object-walker-test/object1.json");
        var jsonWalker = new JsonObjectWalker(Sets.newHashSet(new JsonObjectWalker(), new JsonArrayWalker()));
        AtomicInteger processedCount = new AtomicInteger();
        
        jsonWalker.handler(event -> {
            processedCount.incrementAndGet();
            
            // End walking after processing 10 items
            if (processedCount.get() >= 10) {
                event.end();
            }
        });
        
        jsonWalker.endHandler(context -> {
            assertEquals(10, processedCount.get());
            assertTrue(context.success());
            checkpoint.flag();
        });
        
        jsonWalker.errorHandler(context -> {
            testContext.failNow(context.cause());
        });
        
        jsonWalker.handle(json);
    }
} 