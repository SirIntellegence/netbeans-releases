/*
 * XTestExecutorBeanInfo.java
 *
 * Created on April 29, 2002, 10:54 AM
 */

package org.netbeans.modules.testtools;

import java.beans.SimpleBeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.BeanDescriptor;
import java.awt.Image;
import org.openide.util.Utilities;
import org.openide.compiler.CompilerType;

/**
 *
 * @author  as103278
 */
public class XTestCompilerTypeBeanInfo extends SimpleBeanInfo {

    public BeanInfo[] getAdditionalBeanInfo () {
        try {
            return new BeanInfo[] {
                       Introspector.getBeanInfo(CompilerType.class)
                   };
        } catch (IntrospectionException ie) {
            if (Boolean.getBoolean ("netbeans.debug.exceptions"))
                ie.printStackTrace ();
            return null;
        }
    }

    public BeanDescriptor getBeanDescriptor () {
        BeanDescriptor desc = new BeanDescriptor (XTestCompilerType.class);
        desc.setDisplayName ("XTest Compilation");
        desc.setShortDescription ("XTest Compilation");
        desc.setValue ("version", "1.1");
        return desc;
    }

    public PropertyDescriptor[] getPropertyDescriptors () {
        try {
            PropertyDescriptor[] properties=new PropertyDescriptor[5];
            properties[0]=new PropertyDescriptor("netbeansHome", XTestCompilerType.class);
            properties[0].setDisplayName("Netbeans Home");
            properties[0].setShortDescription("Netbeans IDE Home Directory (different than current).");
            properties[0].setValue("files",new Boolean(false));
            properties[0].setPreferred(true);
            properties[1]=new PropertyDescriptor("jellyHome", XTestCompilerType.class);
            properties[1].setDisplayName("Jelly Home");
            properties[1].setShortDescription("Jelly Home Directory");
            properties[1].setValue("files",new Boolean(false));
            properties[2]=new PropertyDescriptor("jemmyHome", XTestCompilerType.class);
            properties[2].setDisplayName("Jemmy Home");
            properties[2].setShortDescription("Jemmy Home Directory");
            properties[2].setValue("files",new Boolean(false));
            properties[3]=new PropertyDescriptor("testType", XTestCompilerType.class);
            properties[3].setDisplayName("Test Type");
            properties[3].setShortDescription("Test Type");
            properties[4]=new PropertyDescriptor("xtestHome", XTestCompilerType.class);
            properties[4].setDisplayName("XTest Home");
            properties[4].setShortDescription("XTest Home Directory");
            properties[4].setValue("files",new Boolean(false));
            return properties;
        } catch( IntrospectionException e) {
            if (Boolean.getBoolean ("netbeans.debug.exceptions"))
                e.printStackTrace ();
            e.printStackTrace();
            return null;
        }
    }

    public Image getIcon (int type) {
        if (type == BeanInfo.ICON_COLOR_16x16 || type == BeanInfo.ICON_MONO_16x16) {
            return Utilities.loadImage ("org/apache/tools/ant/module/resources/AntIcon.gif");
        } else {
            return null;
        }
    }
    
}
