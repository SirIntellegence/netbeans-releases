"""
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
*
* Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
* nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
* particular file as subject to the "Classpath" exception as provided
* by Sun in the GPL Version 2 section of the License file that
* accompanied this code. If applicable, add the following below the
* License Header, with the fields enclosed by brackets [] replaced by
* your own identifying information:
* "Portions Copyrighted [year] [name of copyright owner]"
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
*
* Contributor(s):
*
* Portions Copyrighted 2009 Sun Microsystems, Inc.
"""


""" misc utility modules used by jpydbg stuff """

__version__='$Revision: 1.1 $'
__date__='$Date: 2006/05/01 10:17:14 $'
__author__= 'Jean-Yves Mengant'

# $Source: /cvsroot/jpydbg/jpydebugforge/src/python/jpydbg/dbgutils.py,v $

import sys
import traceback
import os
import socket
import string
import threading


class jpyutils :

    def __init__( self ):
        if ( os.name == 'java' ):
            self.isJython = 1
        else:
            self.isJython = 0

    def parsedReturned( self ,
                        command = 'COMMAND' ,
                        argument = None ,
                        message = None ,
                        details = None ):
        parsedCommand = []
        parsedCommand.append(command)
        parsedCommand.append(argument)
        parsedCommand.append(message)
        parsedCommand.append(details)
        return parsedCommand

    def populateCMDException( self , arg , oldstd ):
        "global utility exception reporter for all pydbg classes"
        sys.stdout=oldstd
        tb , exctype , value = sys.exc_info()
        excTrace = traceback.format_exception( tb , exctype , value )
        tb = None # release
        return self.parsedReturned( argument = arg ,
                                    message = "Error on CMD" ,
                                    details = excTrace
                                  )

    def removeForXml( self , strElem , keepLinefeed = 0 ):
        "replace unsuported xml encoding characters"
        if (not  keepLinefeed ):
            strElem = strElem.replace('\n','')
        strElem = strElem.replace('&',"&amp;")
        strElem = strElem.replace('"',"&quot;")
        strElem = strElem.replace('<','&lt;')
        strElem = strElem.replace('>','&gt;')
        # strElem = string.replace(strElem,'&','&amp;')
        return strElem

    def getArg( self , toParse ):
        toParse = toParse.strip()
        if len(toParse) == 0:
            return None
        # check for leading quotes in arguments which implies
        # quoted argument separated by quotes instead of spaces
        if ( toParse[0] == '"' or toParse[0]== "'" ):
            toParse = toParse[1:len(toParse)-1]
        #
        return toParse

    def consumeArgv( self , containing=None ):
        """ consume requested sys.argv and return its value back """
        if (len(sys.argv) > 1):
            returned = sys.argv[1]
            if ( containing != None ):
                # check matching
                if returned.find(containing) == -1:
                    return None #don't match
            #  consume and return value
            sys.argv =  [sys.argv[0]] + sys.argv[2:]
            return returned
        else:
            return None

    def nextArg( self , toParse ):
        """ get next arg back on command buffer """
        if toParse == None :
            return None , None
        toParse = string.strip(toParse)
        separator = " "
        if len(toParse) == 0:
            return None , None
        # check for leading quotes in arguments which implies
        # quoted argument separated by quotes instead of spaces
        if ( toParse[0] == '"' or toParse[0]== "'" ):
            separator = toParse[0]
            toParse = toParse[1:]
        #
        nextSpace = toParse.find(separator)
        if ( nextSpace == -1 ):
            return string.strip(toParse) , None
        else:
            return string.strip(toParse[:nextSpace]) , string.strip(toParse[nextSpace+1:])


class PythonPathHandler:
    "store the python path in a text file for jpydebug usage"
    def __init__(self , pyPathFName):
        self.PyPathFName = pyPathFName

    def getPyPathFromEnv( self ):
        "PYTHONPATH env and set sys.path out of it "
        pyPath = os.environ["PYTHONPATH"]
        # cleanly take care of previous ';' convention
        if os.pathsep != ';':
            pyPath.replace(';' , os.pathsep)
        if pyPath.find(os.pathsep) != -1:
            sys.path = pyPath.split(os.pathsep)
            # remove empty nodes first
            for element in sys.path:
                if ( len(element.strip())==0 ):
                    sys.path.remove(element)

    def setPyPathFileFromPath( self ):
        "save PYTHON sys path in a file"
        try:
            pathStr = ''
            for pathElem in sys.path:
                pathStr += pathElem+os.pathsep
            pyPathFile = open( self.PyPathFName , mode='w' )
            pyPathFile.write(pathStr)
            pyPathFile.close()
        except:
            # go ahead on exception on file access
            pass

_debugPath = os.path.dirname( sys._getframe(0).f_code.co_filename )
_DEBUGLOG = _debugPath + "/jpydbg.log"

class DebugLogger :

  def __init__( self  ) :
      f = file( _DEBUGLOG ,"w") ;
      f.close() # reset log on startup

  def debug( self , toWrite ) :
      f = file( _DEBUGLOG ,"a+") ;
      f.write( toWrite + '\n')
      f.close()

###############################################################################
# do a touch of jpydbg.log in same directory as jpydebug.py to get debug traces on
###############################################################################
_debugLogger = None
if os.path.exists(_DEBUGLOG) :
    _debugLogger = DebugLogger()
    _debugLogger.debug("***** Debug Session Started ******")


class NetworkSession:
    """ handle network session for JpyDbg and Completion engine """

    def __init__( self , connection ) :
        self._connection = connection
        self._lastBuffer = ''
        self.lock = threading.Lock()
        self._acquire_lock = self.lock.acquire
        self._release_lock = self.lock.release

    def _DBG( self , toWrite ):
        if _debugLogger :
            _debugLogger.debug(toWrite)

    def populateToClient( self , bufferList ) :
        """ populate back bufferList to client side """
        self._DBG( "populateXmlToClient --> " + buffer )
        self._connection.send( ''.join(bufferList) )

    def populateXmlToClient( self , bufferList ) :
        """ populate JpyDbg Xml buffer back """
        mbuffer = '<JPY>'
        for element in bufferList:
            mbuffer = mbuffer + ' ' + str(element)
        mbuffer = mbuffer + '</JPY>\n'
        self._DBG( "populateToClient --> " + mbuffer )
        self._connection.send( mbuffer )


    def readNetBuffer( self ):
        """ reading on network socket """
        try:
            if ( self._lastBuffer.find('\n') != -1 ):
                return self._lastBuffer ; # buffer stills contains commands
            networkData = self._connection.recv(1024)
            if not networkData:  # capture network interuptions if any
                return None
            data = self._lastBuffer + networkData
            return data
        except socket.error, (errno,strerror):
            print "recv interupted errno(%s) : %s" % ( errno , strerror )
            return None


    def _receiveCommand( self ):
        """ receive a command back """
        data = self.readNetBuffer() ;
        # data reception from Ip
        while ( data != None and data):
            eocPos = data.find('\n')
            nextPos = eocPos ;
            while (  nextPos < len(data) and \
                   ( data[nextPos] == '\n' or data[nextPos] == '\r') ): # ignore consecutive \n\r
                nextPos = nextPos+1
            if ( eocPos != -1 ): # full command received in buffer
                self._lastBuffer = data[nextPos:] # cleanup received command from buffer
                returned = data[:eocPos]
                self._DBG( "<-- received Command " + returned )
                if (returned[-1] == '\r'):
                    return returned[:-1]
                return returned
            data = self.readNetBuffer() ;
        # returning None on Ip Exception
        return None

    def receiveCommand( self ):
        self._acquire_lock()
        try :
            returned = self._receiveCommand()
        finally :
            self._release_lock()
        return returned


    def close( self ):
        """ close the associated ip session """
        self._DBG( "**** DEBUGGER CONNECTION CLOSED ***" )
        self._connection.close()
