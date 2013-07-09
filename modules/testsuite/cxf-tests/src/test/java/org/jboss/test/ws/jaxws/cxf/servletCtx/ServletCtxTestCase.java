/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.test.ws.jaxws.cxf.servletCtx;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import junit.framework.Test;

import org.jboss.wsf.test.JBossWSCXFTestSetup;
import org.jboss.wsf.test.JBossWSTest;

/**
 *
 * @author alessio.soldano@jboss.com
 * @since 09-Jul-2013
 */
public class ServletCtxTestCase extends JBossWSTest
{
   private String endpointOneURL = "http://" + getServerHost() + ":8080/jaxws-cxf-servletCtx/ServiceOne";

   public static Test suite()
   {
      return new JBossWSCXFTestSetup(ServletCtxTestCase.class, "jaxws-cxf-servletCtx.war");
   }

   private String targetNS = "http://org.jboss.ws.jaxws.cxf/servletCtx";

   private EndpointOne portOne;

   protected int defaultSize = 30;

   public void testAccess() throws Exception
   {
      initPort();
      int count1 = portOne.getCount1();
      int count2 = portOne.getCount2();
      Object retObj = portOne.echo("Hello");
      assertEquals("Hello", retObj);
      assertEquals(1, portOne.getCount1() - count1);
      assertEquals(1, portOne.getCount2() - count2);
   }

   public void testConcurrentInvocations() throws Exception
   {
      runConcurrentTests(false);
   }

   public void testConcurrentOneWayInvocations() throws Exception
   {
      runConcurrentTests(true);
   }

   private void runConcurrentTests(boolean oneway) throws Exception
   {
      initPort();
      final int size = defaultSize;
      int count1 = portOne.getCount1();
      int count2 = portOne.getCount2();
      ExecutorService es = Executors.newFixedThreadPool(size);
      List<Callable<Boolean>> callables = new ArrayList<Callable<Boolean>>(size);
      for (int i = 0; i < size; i++)
      {
         callables.add(new CallableOne(portOne, oneway, i));
      }
      List<Future<Boolean>> futures = es.invokeAll(callables);
      for (Future<Boolean> f : futures)
      {
         assertTrue(f.get());
      }
      if (oneway)
      {
         Thread.sleep(3000);
      }
      assertEquals(size, portOne.getCount1() - count1);
      assertEquals(size, portOne.getCount2() - count2);
   }

   private void initPort() throws MalformedURLException
   {
      URL wsdlOneURL = new URL(endpointOneURL + "?wsdl");
      QName serviceOneName = new QName(targetNS, "ServiceOne");
      Service serviceOne = Service.create(wsdlOneURL, serviceOneName);
      portOne = (EndpointOne) serviceOne.getPort(EndpointOne.class);
   }

   private static class CallableOne implements Callable<Boolean>
   {
      private EndpointOne port;

      private boolean oneway;

      private int seqNum;

      public CallableOne(EndpointOne port, boolean oneway, int seqNum)
      {
         this.port = port;
         this.oneway = oneway;
         this.seqNum = seqNum;
      }

      public Boolean call() throws Exception
      {
         String arg = "Foo" + seqNum;
         if (oneway)
         {
            port.echoOneWay(arg);
            return true;
         }
         else
         {
            String result = port.echo(arg);
            return arg.equals(result);
         }
      }
   }

}
