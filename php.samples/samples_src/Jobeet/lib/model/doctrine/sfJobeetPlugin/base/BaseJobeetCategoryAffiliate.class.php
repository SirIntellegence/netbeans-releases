<?php

/**
 * This class has been auto-generated by the Doctrine ORM Framework
 */
abstract class BaseJobeetCategoryAffiliate extends sfDoctrineRecord
{
  public function setTableDefinition()
  {
    $this->setTableName('jobeet_category_affiliate');
    $this->hasColumn('category_id', 'integer', null, array('type' => 'integer', 'primary' => true));
    $this->hasColumn('affiliate_id', 'integer', null, array('type' => 'integer', 'primary' => true));
  }

  public function setUp()
  {
    $this->hasOne('JobeetCategory', array('local' => 'category_id',
                                          'foreign' => 'id',
                                          'onDelete' => 'CASCADE'));

    $this->hasOne('JobeetAffiliate', array('local' => 'affiliate_id',
                                           'foreign' => 'id',
                                           'onDelete' => 'CASCADE'));
  }
}