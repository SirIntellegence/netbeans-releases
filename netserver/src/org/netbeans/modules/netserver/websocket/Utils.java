/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.netserver.websocket;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;


/**
 * @author ads
 *
 */
final class Utils {
    
    public static final String UTF_8 = "UTF-8";                    // NOI18N
    private static final char NEW_LINE = '\n';
    public static final int BYTES = 1000;
    
    static final String HTTP_11 = "HTTP/1.1";                       // NOI18N
    
    static final String HTTP_RESPONSE = HTTP_11+" 101 Web Socket Protocol Handshake"; // NOI18N
    
    static final String GET = "GET";
    
    static final String WS_UPGRADE = "Upgrade: WebSocket";                            // NOI18N
    
    static final String WS_UPGRADE_1 = "Upgrade: websocket";                          // NOI18N
    
    static final String CONN_UPGRADE = "Connection: Upgrade";                         // NOI18N
    
    static final String CRLF = "\r\n";                                                // NOI18N
    
    static final String HOST = "Host";                                                // NOI18N
    
    static final String WS_PROTOCOL = "WebSocket-Protocol";                           // NOI18N
    
    static final String VERSION = "Sec-WebSocket-Version";  // NOI18N
    static final String KEY = "Sec-WebSocket-Key";          // NOI18N
    static final String KEY1 = "Sec-WebSocket-Key1";        // NOI18N
    static final String KEY2 = "Sec-WebSocket-Key2";        // NOI18N
    static final String ACCEPT = "Sec-WebSocket-Accept";    // NOI18N
    
    private Utils(){
    }
    
    static List<String> readHttpRequest(SocketChannel socketChannel,
            ByteBuffer buffer ) throws IOException
    {
        return readHttpRequest(socketChannel, buffer, null );
    }
    
    static List<String> readHttpRequest(SocketChannel socketChannel,
            ByteBuffer buffer , byte[] content) throws IOException
    {
            List<String> headers = new LinkedList<String>();
            buffer.clear();
            StringBuilder builder = new StringBuilder();
            byte[] bytes = new byte[ BYTES ];
            boolean readContent = content != null;
            List<Byte> remaining = new LinkedList<Byte>();
            read: while( true ){
                int read = socketChannel.read( buffer );
                if ( read ==-1 ){
                    return null;
                }
                buffer.flip();
                int size = buffer.limit();
                buffer.get( bytes , 0, size);
                buffer.clear();
                String stringValue = new String( bytes , 0, size, 
                        Charset.forName(UTF_8) );
                String fullString = stringValue;
                int index = stringValue.indexOf(NEW_LINE);
                if ( index == -1 ){
                    builder.append( stringValue );
                    if ( readContent ){
                        copyBytes(bytes, remaining, 0, size);
                    }
                }
                else {
                    if ( readContent ){
                        remaining = new LinkedList<Byte>();
                    }
                    builder.append( stringValue.subSequence(0, index));
                    String line = builder.toString().trim();
                    headers.add( line );
                    builder.setLength(0);
                    if ( line.isEmpty() ){
                        int start = stringValue.substring(0, index+1).getBytes().length;
                        copyBytes(bytes, remaining, start, size );
                        break;
                    }
                    int fullIndex = index;
                    do {
                        stringValue = stringValue.substring( index +1);
                        index = stringValue.indexOf(NEW_LINE );
                        if ( index != -1){
                            fullIndex+=(index+1);
                            line = stringValue.substring( 0, index ).trim();
                            headers.add( line );
                            if ( line.isEmpty() ){
                                int start = fullString.substring(0, fullIndex+1).
                                        getBytes().length;
                                copyBytes(bytes, remaining, start, size );
                                break read;
                            }
                        }
                    }
                    while( index != -1 );
                    int start = fullString.substring(0, fullIndex+1).getBytes().length;
                    copyBytes(bytes, remaining, start, size );
                    builder.append( stringValue);
                }
            }

            if ( remaining.size() == 0 ){
                return headers;
            }
            if ( !readContent ){
                throw new IOException("Unexpected content on connection initialization");       // NOI18N
            }
            else {
                int size = content.length;
                int red = remaining.size();
                if ( red > size ){
                    throw new IOException("Unexpected content on connection initialization");       // NOI18N
                }
                ByteBuffer buf = ByteBuffer.allocate( size - red );
                while(red<size){
                    int read = socketChannel.read( buffer );
                    if ( read == -1){
                        return null;
                    }
                    red+=read;
                }
                buf.flip();
                bytes = new byte[buf.capacity()];
                buf.get(bytes);
                int i=0;
                for( Byte b: remaining ){
                    content[i] = b;
                    i++;
                }
                System.arraycopy(bytes, 0, content, i, bytes.length );
            }
            return headers;
    }
    
    private static void copyBytes( byte[] src, List<Byte> dst , int startPos , 
            int lenght)
    {
        for( int i=startPos; i< lenght ; i++ ){
            dst.add( src[i]);
        }
    }
    
    static String getOrigin(URI uri ){
        String url = uri.toString();
        String host = uri.getHost();
        int index = url.indexOf(host);
        if ( index != -1 ){
            return url.substring( 0, index+host.length());
        }
        else {
            return uri.getScheme()+"://"+uri.getHost();
        }
    }

}
