/*
 * Copyright (c) 2007, Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of Sun Microsystems, Inc. nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */


package enterprise.customer_cmp_appclient;

import javax.naming.InitialContext;
import java.util.List;
import javax.ejb.EJB;
import enterprise.customer_cmp_ejb.ejb.session.CustomerSessionRemote;
import enterprise.customer_cmp_ejb.persistence.*;
        
public class CustomerAppClient {

    @EJB 
    private static CustomerSessionRemote sess;

    public static void main(String args[]) {
	try {
		InitialContext ic = new InitialContext();
                String CUSTOMER_ID="99999";
                
                System.out.println("Searching for customer with id:"+CUSTOMER_ID);
                Customer searchedCustomer= sess.searchForCustomer(CUSTOMER_ID);
                
                if(searchedCustomer==null){
                    throw new Exception("searched customer not found");
                }
                
                System.out.println("found customer with id:"+CUSTOMER_ID);
                System.out.println("First Name:"+searchedCustomer.getFirstName());
                System.out.println("Last Name:"+searchedCustomer.getLastName());
                
	} catch(Exception e) {
		e.printStackTrace();
	}
  }

}