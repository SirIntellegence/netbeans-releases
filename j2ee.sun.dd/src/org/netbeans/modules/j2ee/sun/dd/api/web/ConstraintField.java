/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
/*
 * ConstraintField.java
 *
 * Created on November 15, 2004, 4:26 PM
 */
package org.netbeans.modules.j2ee.sun.dd.api.web;

public interface ConstraintField extends org.netbeans.modules.j2ee.sun.dd.api.CommonDDBean {

        public static final String NAME = "Name";	// NOI18N
	public static final String SCOPE = "Scope";	// NOI18N
	public static final String CACHEONMATCH = "CacheOnMatch";	// NOI18N
	public static final String CACHEONMATCHFAILURE = "CacheOnMatchFailure";	// NOI18N
	public static final String CONSTRAINT_FIELD_VALUE = "ConstraintFieldValue";	// NOI18N
	public static final String CONSTRAINTFIELDVALUEMATCHEXPR = "ConstraintFieldValueMatchExpr";	// NOI18N
	public static final String CONSTRAINTFIELDVALUECACHEONMATCH = "ConstraintFieldValueCacheOnMatch";	// NOI18N
	public static final String CONSTRAINTFIELDVALUECACHEONMATCHFAILURE = "ConstraintFieldValueCacheOnMatchFailure";	// NOI18N

        /** Setter for name attribute 
         * @param value attribute value
         */
	public void setName(java.lang.String value);
        /** Getter for name attribute 
         * @return attribute value
         */    
	public java.lang.String getName();
        /** Setter for scope attribute 
         * @param value attribute value
         */
	public void setScope(java.lang.String value);
        /** Getter for scope attribute 
         * @return attribute value
         */    
	public java.lang.String getScope();
        /** Setter for cache-on-match attribute 
         * @param value attribute value
         */
	public void setCacheOnMatch(java.lang.String value);
        /** Getter for cache-on-match attribute 
         * @return attribute value
         */
	public java.lang.String getCacheOnMatch();
        /** Setter for cache-on-match-failure attribute 
         * @param value attribute value
         */
	public void setCacheOnMatchFailure(java.lang.String value);
        /** Getter for cache-on-match-failure attribute 
         * @return attribute value
         */
	public java.lang.String getCacheOnMatchFailure();

	public void setConstraintFieldValue(int index, String value);
	public String getConstraintFieldValue(int index);
	public int sizeConstraintFieldValue();
	public void setConstraintFieldValue(String[] value);
	public String[] getConstraintFieldValue();
	public int addConstraintFieldValue(String value);
	public int removeConstraintFieldValue(String value);

        /** Setter for match-expr attribute of constraint-field-value
         * @param value attribute value
         */
        public void setConstraintFieldValueMatchExpr(int index, java.lang.String value);
        /** Getter for match-expr attribute of constraint-field-value
         * @return attribute value
         */
	public java.lang.String getConstraintFieldValueMatchExpr(int index);

	public int sizeConstraintFieldValueMatchExpr();
        /** Setter for cache-on-match attribute of constraint-field-value
         * @param value attribute value
         */
	public void setConstraintFieldValueCacheOnMatch(int index, java.lang.String value);
        /** Getter for cache-on-match attribute of constraint-field-value
         * @return attribute value
         */
	public java.lang.String getConstraintFieldValueCacheOnMatch(int index);

	public int sizeConstraintFieldValueCacheOnMatch();
        /** Setter for cache-on-match-failure attribute of constraint-field-value
         * @param value attribute value
         */
	public void setConstraintFieldValueCacheOnMatchFailure(int index, java.lang.String value);
        /** Getter for cache-on-match-failure attribute of constraint-field-value
         * @return attribute value
         */
	public java.lang.String getConstraintFieldValueCacheOnMatchFailure(int index);

	public int sizeConstraintFieldValueCacheOnMatchFailure();

}
