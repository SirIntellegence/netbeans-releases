<?php
if (!isset($order[$by])) {
    $order[$by] = 'a';
} elseif ($order[$by] === 'd') {
    unset($order[$by]);
} else {
    $order[$by] = 'd';
}
?>