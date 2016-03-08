//
//  ========================================================================
//  Copyright (c) 1995-2016 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.common.functions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketFrame;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.eclipse.jetty.websocket.common.InvalidSignatureException;
import org.eclipse.jetty.websocket.common.WebSocketFrame;
import org.eclipse.jetty.websocket.common.util.DynamicArgs;
import org.eclipse.jetty.websocket.common.util.DynamicArgs.Arg;
import org.eclipse.jetty.websocket.common.util.ExactSignature;
import org.eclipse.jetty.websocket.common.util.ReflectUtils;

/**
 * Jetty {@link WebSocket} {@link OnWebSocketFrame} method {@link Function}
 */
public class OnFrameFunction implements Function<Frame, Void>
{
    private static final DynamicArgs.Builder ARGBUILDER;
    private static final Arg SESSION = new Arg(1,Session.class);
    private static final Arg FRAME = new Arg(2,Frame.class);

    static
    {
        ARGBUILDER = new DynamicArgs.Builder();
        ARGBUILDER.addSignature(new ExactSignature(Frame.class));
        ARGBUILDER.addSignature(new ExactSignature(Session.class,Frame.class));
    }

    private final Session session;
    private final Object endpoint;
    private final Method method;
    private final DynamicArgs callable;

    public OnFrameFunction(Session session, Object endpoint, Method method)
    {
        this.session = session;
        this.endpoint = endpoint;
        this.method = method;

        ReflectUtils.assertIsAnnotated(method,OnWebSocketFrame.class);
        ReflectUtils.assertIsPublicNonStatic(method);
        ReflectUtils.assertIsReturn(method,Void.TYPE);

        this.callable = ARGBUILDER.build(method,SESSION,FRAME);
        if (this.callable == null)
        {
            throw InvalidSignatureException.build(method,OnWebSocketFrame.class,ARGBUILDER);
        }
    }

    @Override
    public Void apply(Frame frame)
    {
        WebSocketFrame copy = WebSocketFrame.copy(frame);
        try
        {
            this.callable.invoke(endpoint,session,copy);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            throw new WebSocketException("Unable to call frame method " + ReflectUtils.toString(endpoint.getClass(),method),e);
        }
        return null;
    }
}