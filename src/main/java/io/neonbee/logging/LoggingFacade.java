package io.neonbee.logging;

import static io.neonbee.internal.handler.CorrelationIdHandler.CORRELATION_ID;
import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.neonbee.data.DataContext;
import io.neonbee.logging.internal.LoggingFacadeImpl;
import io.vertx.ext.web.RoutingContext;

public interface LoggingFacade extends Logger {
    /**
     * Masquerades an existing SLF4J logger and adds functionality to correlate given log messages with a correlation
     * id. Here is a simple example on how to use the facade:
     *
     * <code>
     * private static final LoggingFacade LOGGER = LoggingFacade.masqueradeLogger(
     *         LoggerFactory.getLogger(MethodHandles.lookup().lookupClass()));
     * </code>
     *
     * In your coding you can then correlate any log message with a given correlationId:
     *
     * <code>
     * LOGGER.correlateWith("any correlation id").debug("Hello World");
     * </code>
     *
     * Please be aware, that by masquerading the logger, you will loose the ability to be working with makers when using
     * the methods this facade exposes. Any attempts to be invoking a method with a marker will result in a
     * {@link UnsupportedOperationException}.
     *
     * Note: After the log message was written, the correlation id will be removed from the facade by the log delegate.
     * This guarantees that subsequent log messages will not mix up correlation ids, even if they are executed on a
     * different thread. This means the {@link #correlateWith(String)} method has to be called each time a log message
     * should be printed with a correlation id associated.
     *
     * @param logger an instance of {@link Logger}
     * @return a logging facade, offering ways to correlate the logged messages
     */
    static LoggingFacade masqueradeLogger(Logger logger) {
        return new LoggingFacadeImpl(logger);
    }

    /**
     * Creates a new {@link Logger} with the passed name and {@link #masqueradeLogger(Logger) masquerade} it.
     *
     * @param name The name of the new logger.
     * @return a logging facade, offering ways to correlate the logged messages
     */
    static LoggingFacade create(String name) {
        return masqueradeLogger(LoggerFactory.getLogger(name));
    }

    /**
     * Creates a new {@link Logger} with the FQN of the passed class and {@link #masqueradeLogger(Logger) masquerade}
     * it.
     *
     * @param clazz Class name of the new logger.
     * @return a logging facade, offering ways to correlate the logged messages
     */
    static LoggingFacade create(Class<?> clazz) {
        return masqueradeLogger(LoggerFactory.getLogger(clazz));
    }

    /**
     * Creates a new {@link Logger} with the FQN of the class which calls this method and
     * {@link #masqueradeLogger(Logger) masquerade} it.
     *
     * @return a logging facade, offering ways to correlate the logged messages
     */
    static LoggingFacade create() {
        StackWalker walker = StackWalker.getInstance(RETAIN_CLASS_REFERENCE);
        Class<?> callerClass = walker.walk(stackStream -> stackStream
                .filter(stackframe -> !LoggingFacade.class.equals(stackframe.getDeclaringClass())).findFirst().get()
                .getDeclaringClass());

        return create(callerClass);
    }

    /**
     * This method correlates the next call to this logger with a given correlation id
     *
     * Note: After the logger logged anything the correlation id will be removed, to not mix up correlation ids. This
     * means in case you want to log multiple messages consecutively you will have to call the correlateWith method
     * providing the correlation id again. Similarly you should never call the correlateWith method and then not
     * actually trigger a log message, as this will cause a "pollution" of the logging facade.
     *
     * @param correlationId the correlationId to associate the next log message with
     * @return the logging facade for method chaining
     */
    LoggingFacade correlateWith(String correlationId);

    /**
     * Convenience method to set the correlation id of a routing context to the logging facade.
     *
     * @see #correlateWith(String)
     * @param routingContext any routing context
     * @return the logging facade for method chaining
     */
    @SuppressWarnings("PMD.AvoidThrowingNullPointerException")
    default LoggingFacade correlateWith(RoutingContext routingContext) {
        if (routingContext == null) {
            String msg = "routingContext must not be null, otherwise no correlationId can be extracted";
            throw new NullPointerException(msg);
        }
        Optional.ofNullable(routingContext.get(CORRELATION_ID)).map(Object::toString)
                .ifPresent(correlationId -> correlateWith(correlationId));
        return this;
    }

    /**
     * Convenience method to set the correlation id of a data context to the logging facade.
     *
     * @see #correlateWith(String)
     * @param context any data context
     * @return the logging facade for method chaining
     */

    default LoggingFacade correlateWith(DataContext context) {
        return correlateWith(context.correlationId());
    }
}
