package com.example.salesbatch.workflow;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.servlet.MultipartConfigElement;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import com.example.salesbatch.workflow.config.AppConfig;
import com.example.salesbatch.workflow.config.InfraConfig;
import com.example.salesbatch.workflow.config.WebConfig;
import com.example.salesbatch.workflow.web.ApproverAuth;

/** Starts embedded Jetty with the Spring root context (engine, delegates) and the MVC servlet. */
public class WorkflowServer {

    /** Listens on the loopback address unless -Dserver.host says otherwise. */
    static final String DEFAULT_HOST = "127.0.0.1";

    /** Largest upload accepted, in bytes; override with -Dupload.max.bytes. */
    static final long DEFAULT_UPLOAD_LIMIT = 50L * 1024 * 1024;

    public static void main(String[] args) throws Exception {
        String host = System.getProperty("server.host", DEFAULT_HOST);
        requireApproversForRemoteBind(host, new ApproverAuth());
        Server server = createServer(host, Integer.getInteger("server.port", 8080), AppConfig.class, InfraConfig.class);
        server.start();
        server.join();
    }

    /**
     * Refuses to listen beyond the loopback address while anyone could approve a batch. Set -Dapprovers
     * (or APPROVERS) to "name:token,name:token" to listen on another address.
     */
    static void requireApproversForRemoteBind(String host, ApproverAuth approverAuth) {
        if (!approverAuth.enabled() && !isLoopback(host)) {
            throw new IllegalStateException("Refusing to listen on " + host + " without approver authentication. "
                    + "Set -Dapprovers=name:token (or APPROVERS) or listen on " + DEFAULT_HOST + ".");
        }
    }

    static boolean isLoopback(String host) {
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /** Same as {@link #createServer(String, int, Class[])} on the default (loopback) address. */
    public static Server createServer(int port, Class<?>... rootConfigs) {
        return createServer(System.getProperty("server.host", DEFAULT_HOST), port, rootConfigs);
    }

    /**
     * Builds, but does not start, the server. The root context is made of the given configuration classes
     * (the real app uses AppConfig and InfraConfig; tests swap InfraConfig for fakes).
     */
    public static Server createServer(String host, int port, Class<?>... rootConfigs) {
        AnnotationConfigWebApplicationContext root = new AnnotationConfigWebApplicationContext();
        root.register(rootConfigs);

        AnnotationConfigWebApplicationContext web = new AnnotationConfigWebApplicationContext();
        web.register(WebConfig.class);

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        handler.setContextPath("/");
        handler.addEventListener(new ContextLoaderListener(root));
        ServletHolder dispatcher = new ServletHolder(new DispatcherServlet(web));
        long limit = Long.getLong("upload.max.bytes", DEFAULT_UPLOAD_LIMIT);
        dispatcher.getRegistration().setMultipartConfig(new MultipartConfigElement(
                System.getProperty("java.io.tmpdir"), limit, limit + 1024 * 1024, 1024 * 1024));
        handler.addServlet(dispatcher, "/*");

        Server server = new Server(new InetSocketAddress(host, port));
        server.setHandler(handler);
        return server;
    }
}
