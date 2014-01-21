package org.arnulfo.jettyembed;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import static com.codahale.metrics.MetricRegistry.name;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ArrayBlockingQueue;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Hello world!
 *
 */
public class App {

  public static class MyServlet extends HttpServlet {

    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
      response.setContentType("text/plain");
      response.getWriter().write(getClass().getName() + " - OK");
    }
  }

  public static void main(String[] args) throws Exception {
    final MetricRegistry registry = new MetricRegistry();

    Server server = new Server(8080);
    final ArrayBlockingQueue<Runnable> arrayBlockingQueue = new ArrayBlockingQueue<Runnable>(10);
    final QueuedThreadPool queuedThreadPool = new QueuedThreadPool(arrayBlockingQueue);

    registry.register(name(QueuedThreadPool.class, "taille"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return queuedThreadPool.getThreads();
      }
    });
    
    registry.register(name(ArrayBlockingQueue.class, "taille"), new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return arrayBlockingQueue.size();
      }
    });
    
    final JmxReporter reporter = JmxReporter.forRegistry(registry).build();
    reporter.start();

    server.setThreadPool(queuedThreadPool);

    MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.getContainer().addEventListener(mbContainer);
    server.addBean(mbContainer);
    ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    handler.setContextPath("/"); // technically not required, as "/" is the default
    handler.addServlet(MyServlet.class, "/communication-service");
    server.setHandler(handler);
    server.start();
  }
}
