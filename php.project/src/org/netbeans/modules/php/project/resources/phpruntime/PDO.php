<?php

// Start of PDO v.1.0.4dev

class PDOException extends RuntimeException  {
	protected $message;
	protected $code;
	protected $file;
	protected $line;
	public $errorInfo;


	final private function __clone () {}

	/**
	 * @param message[optional]
	 * @param code[optional]
	 */
	public function __construct ($message, $code) {}

	final public function getMessage () {}

	final public function getCode () {}

	final public function getFile () {}

	final public function getLine () {}

	final public function getTrace () {}

	final public function getTraceAsString () {}

	public function __toString () {}

}

class PDO  {
	const PARAM_BOOL = 5;
	const PARAM_NULL = 0;
	const PARAM_INT = 1;
	const PARAM_STR = 2;
	const PARAM_LOB = 3;
	const PARAM_STMT = 4;
	const PARAM_INPUT_OUTPUT = -2147483648;
	const PARAM_EVT_ALLOC = 0;
	const PARAM_EVT_FREE = 1;
	const PARAM_EVT_EXEC_PRE = 2;
	const PARAM_EVT_EXEC_POST = 3;
	const PARAM_EVT_FETCH_PRE = 4;
	const PARAM_EVT_FETCH_POST = 5;
	const PARAM_EVT_NORMALIZE = 6;
	const FETCH_LAZY = 1;
	const FETCH_ASSOC = 2;
	const FETCH_NUM = 3;
	const FETCH_BOTH = 4;
	const FETCH_OBJ = 5;
	const FETCH_BOUND = 6;
	const FETCH_COLUMN = 7;
	const FETCH_CLASS = 8;
	const FETCH_INTO = 9;
	const FETCH_FUNC = 10;
	const FETCH_GROUP = 65536;
	const FETCH_UNIQUE = 196608;
	const FETCH_KEY_PAIR = 12;
	const FETCH_CLASSTYPE = 262144;
	const FETCH_SERIALIZE = 524288;
	const FETCH_PROPS_LATE = 1048576;
	const FETCH_NAMED = 11;
	const ATTR_AUTOCOMMIT = 0;
	const ATTR_PREFETCH = 1;
	const ATTR_TIMEOUT = 2;
	const ATTR_ERRMODE = 3;
	const ATTR_SERVER_VERSION = 4;
	const ATTR_CLIENT_VERSION = 5;
	const ATTR_SERVER_INFO = 6;
	const ATTR_CONNECTION_STATUS = 7;
	const ATTR_CASE = 8;
	const ATTR_CURSOR_NAME = 9;
	const ATTR_CURSOR = 10;
	const ATTR_ORACLE_NULLS = 11;
	const ATTR_PERSISTENT = 12;
	const ATTR_STATEMENT_CLASS = 13;
	const ATTR_FETCH_TABLE_NAMES = 14;
	const ATTR_FETCH_CATALOG_NAMES = 15;
	const ATTR_DRIVER_NAME = 16;
	const ATTR_STRINGIFY_FETCHES = 17;
	const ATTR_MAX_COLUMN_LEN = 18;
	const ATTR_EMULATE_PREPARES = 20;
	const ATTR_DEFAULT_FETCH_MODE = 19;
	const ERRMODE_SILENT = 0;
	const ERRMODE_WARNING = 1;
	const ERRMODE_EXCEPTION = 2;
	const CASE_NATURAL = 0;
	const CASE_LOWER = 2;
	const CASE_UPPER = 1;
	const NULL_NATURAL = 0;
	const NULL_EMPTY_STRING = 1;
	const NULL_TO_STRING = 2;
	const ERR_NONE = 00000;
	const FETCH_ORI_NEXT = 0;
	const FETCH_ORI_PRIOR = 1;
	const FETCH_ORI_FIRST = 2;
	const FETCH_ORI_LAST = 3;
	const FETCH_ORI_ABS = 4;
	const FETCH_ORI_REL = 5;
	const CURSOR_FWDONLY = 0;
	const CURSOR_SCROLL = 1;
	const MYSQL_ATTR_USE_BUFFERED_QUERY = 1000;
	const MYSQL_ATTR_LOCAL_INFILE = 1001;
	const MYSQL_ATTR_INIT_COMMAND = 1002;
	const MYSQL_ATTR_READ_DEFAULT_FILE = 1003;
	const MYSQL_ATTR_READ_DEFAULT_GROUP = 1004;
	const MYSQL_ATTR_MAX_BUFFER_SIZE = 1005;
	const MYSQL_ATTR_DIRECT_QUERY = 1006;
	const PGSQL_ATTR_DISABLE_NATIVE_PREPARED_STATEMENT = 1000;


	public function __construct () {}

	public function prepare () {}

	public function beginTransaction () {}

	public function commit () {}

	public function rollBack () {}

	public function setAttribute () {}

	public function exec () {}

	public function query () {}

	public function lastInsertId () {}

	public function errorCode () {}

	public function errorInfo () {}

	public function getAttribute () {}

	public function quote () {}

	final public function __wakeup () {}

	final public function __sleep () {}

	public static function getAvailableDrivers () {}

}

class PDOStatement implements Traversable {
	public $queryString;


	public function execute () {}

	public function fetch () {}

	/**
	 * @param var1
	 * @param var2
	 */
	public function bindParam ($var1, &$var2) {}

	/**
	 * @param var1
	 * @param var2
	 */
	public function bindColumn ($var1, &$var2) {}

	public function bindValue () {}

	public function rowCount () {}

	public function fetchColumn () {}

	public function fetchAll () {}

	public function fetchObject () {}

	public function errorCode () {}

	public function errorInfo () {}

	public function setAttribute () {}

	public function getAttribute () {}

	public function columnCount () {}

	public function getColumnMeta () {}

	public function setFetchMode () {}

	public function nextRowset () {}

	public function closeCursor () {}

	public function debugDumpParams () {}

	final public function __wakeup () {}

	final public function __sleep () {}

}

final class PDORow  {
}

function pdo_drivers () {}

// End of PDO v.1.0.4dev
?>
