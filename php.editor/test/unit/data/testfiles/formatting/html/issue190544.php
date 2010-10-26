<div>
<h1><?= $this->translate( 'voc_projects' ) ?></h1>
<?= $this->form; ?>
<? if ($this->projekte) : ?>
<? foreach ($this->projekte as $u) : ?>
<div id="listProjects">
<div class="first">
<div>
<h3>
<strong><?= $u->PKID; ?> </strong>
<a href="/projekte/show/id/<?= $u->PKID ?>"><?= $u->Name; ?></a>
</h3>
</div>
<div class="betreuer">
<br>
<?= $u->o_betreuer1->Vorname ?> <?= $u->o_betreuer1->Name ?> (<?= $this->translate( 'voc_supervisor' ) ?>)
<br>
<?= $u->o_betreuer2->Vorname ?> <?= $u->o_betreuer2->Name ?> (<?= $this->translate( 'voc_cosupervisor' ) ?>)
</div>
</div>
<div id="second<?= $u->PKID ?>" class="second">
</div>
<div class="third">
<div>
<? if ($u->ol_schueler->count()) : ?>
<a href="Javascript: void(0);" onclick="showPassfotos(<?= $u->PKID ?>);">
<img class="icon" src="/images/icons/user9_16x16.gif" alt="user" />
</a>
<? endif; ?>
</div>
</div>
<div>
<div style="float: left;">
<? if ($u->Gedruckt) : ?>
<a href="Javascript: void(0);" onclick="return confirm('<?= $this->translate( 'msg_areyousure' ) ?>') && setGedruckt(<?= $u->PKID ?>);">
<img id="imgPrint_<?= $u->PKID ?>" class="icon" src="/images/icons/print-no_16x16.gif" alt="print" />
</a>
<? else : ?>
<a href="Javascript: void(0);" onclick="return confirm('<?= $this->translate( 'msg_areyousure' ) ?>') && setGedruckt(<?= $u->PKID ?>);">
<img id="imgPrint_<?= $u->PKID ?>" class="icon" src="/images/icons/print_16x16.gif" alt="print" />
</a>
<? endif; ?>
<? if ($u->Gesperrt) : ?>
<a href="Javascript: void(0);" onclick="return confirm('<?= $this->translate( 'msg_areyousure' ) ?>') && setGesperrt(<?= $u->PKID ?>);">
<img id="imgLock_<?= $u->PKID ?>" class="icon" src="/images/icons/lock_16x16.gif" alt="lock" />
</a>
<? else : ?>
<a href="Javascript: void(0);" onclick="return confirm('<?= $this->translate( 'msg_areyousure' ) ?>') && setGesperrt(<?= $u->PKID ?>);">
<img id="imgLock_<?= $u->PKID ?>" class="icon" src="/images/icons/unlock_16x16.gif" alt="unlock" />
</a>
<? endif; ?>
</div>
<div style="text-align: right;">
<? if (($u->IstProjektPerson || in_array( enumRole::ADMIN_PROJEKT, $this->roles )) && !$u->Geschlossen) : ?>
<a href="/projekte/save/id/<?= $u->PKID ?>">
<img class="icon" src="/images/icons/edit_16x16.gif" alt="edit" />
</a>
<? endif; ?>
<? if ($u->IstBesitzer || in_array( enumRole::ADMIN_PROJEKT, $this->roles )) : ?>
<a href="/projekte/delete/id/<?= $u->PKID ?>" onclick="return confirm('<?= $this->translate( 'msg_areyousure' ) ?>')" >
<img class="icon" src="/images/icons/trash_(delete)_16x16.gif" alt="delete" />
</a>
<? endif; ?>
</div>
</div>
</div>
<? endforeach ?>
<? endif; ?>
</div>
