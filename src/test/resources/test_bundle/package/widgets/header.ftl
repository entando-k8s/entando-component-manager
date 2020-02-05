<#assign wp=JspTaglibs[ "/aps-core"]>
<div class="container">
	<header class="container">
		<h1 class="nocrawl">INAIL - Istituto Nazionale per l'Assicurazione contro gli Infortuni sul Lavoro</h1>
		<a href="https://www.inail.it/cs/internet/home.html" title="torna alla homepage INAIL">
			<img src="<@wp.resourceURL />inail/img/LogoInail.svg"
				 alt="INAIL - Istituto Nazionale per l'Assicurazione contro gli Infortuni sul Lavoro"
				 id="logo-header" class="img-responsive">
		</a>
		<h2 id="navinterna" class="sr-only">Menu di navigazione interna alla pagina</h2>
		<ul class="sr-only">
			<li><a accesskey="0" href="#textWrite" title="vai alla ricerca nel sito">Ricerca</a> (accesskey: 0)</li>
			<li><a accesskey="1" href="#contenutoprincipale" title="vai al contenuto di questa pagina">Contenuto della
					pagina</a> (accesskey: 1)
			</li>
			<li><a accesskey="2" href="#navigazioneprincipale" title="vai al menù di navigazione principale del portale">Navigazione principale del portale</a> (accesskey: 2)
			</li>
			<li><a accesskey="3" href="#navigazionesecondaria" title="vai al menù di navigazione di sezione ">Navigazione
					di sezione</a> (accesskey: 3)
			</li>
			<li><a accesskey="4" href="#menusecondario" title="vai al menu utente post login">Menu post login</a> (accesskey: 4)
			</li>
			<li><a accesskey="5" href="#navigazionealternativa" title="vai al menù di accesso rapido ai contenuti di portale">Navigazione alternativa</a> (accesskey: 5)
			</li>
			<li><a accesskey="6" href="#menuservizio" title="vai ai servizi e informazioni">Servizi e informazioni</a> (accesskey: 6)
			</li>
			<li><a accesskey="7" href="#menusocial" title="INAIL nei Social Network">INAIL nei Social Network</a> (accesskey: 7)
			</li>
			<li><a accesskey="l" href="#menulingua" title="Language version">Language version</a> (accesskey: l)</li>
		</ul>
		<hr class="hidden">
		<nav class="navbar navbar-expand-lg has-megamenu">
			<button class="custom-navbar-toggler" type="button" aria-controls="nav1" aria-expanded="false" aria-label="Toggle navigation" data-target="#menuprincipale">
				<svg class="icon"><img src="<@wp.resourceURL />inail/img/sprite.svg#it-burger"></use>
				</svg>
			</button>
			<div class="navbar-collapsable" id="menuprincipale" style="display: none;">
				<div class="overlay" style="display: none;"></div>
				<div class="close-div sr-only">
					<button class="btn close-menu" type="button"><span class="it-close"></span>close</button>
				</div>
				<form method="get" enctype="application/x-www-form-urlencoded" action="https://www.inail.it/cs/internet/risultati_ricerca.html" id="searchForm" name="searchForm">
					<div class="navbar-form navbar-left searchsite" role="search" id="formRicerca">
						<h2 class="hidden">
							Cerca nel sito</h2>
						<p class="form-group">
							<label class="hidden" for="textWrite"></label>
							<input type="text" id="textWrite" name="textWrite" class="form-control ui-autocomplete-input" placeholder="Cerca nel portale" title="inserisci il parametro da cercare su tutto il sito" autocomplete="off">
							<input type="hidden" id="textToFind" name="textToFind" value="">
							<button type="submit" class="btn btn-default">Cerca</button>
						</p>
						<p class="hidden"><a href="#navinterna" title="torna alla navigazione interna alla pagina">Torna
								alla navigazione interna</a></p>
					</div>
				</form>
				<h2 class="hidden">Menu di navigazione principale</h2>
				<div class="menu-wrapper">
					<ul id="navigazioneprincipale" role="menu" class="navbar-nav">
						<li class="dropdown nav-item"><a href="https://www.inail.it/cs/internet/istituto.html" class="dropdown-toggle nav-link" data-toggle="dropdown" role="button" aria-expanded="false"> Istituto <span
										class="sr-only">                                    (selezionato)                                </span>
							</a>
							<div class="dropdown-menu">
								<div class="visible-sm-block visible-md-block visible-lg-block">
									<h3>Istituto</h3>
									<p> Presentazione dell’Ente, della sua organizzazione amministrativa e territoriale, delle relazioni con altri enti e con organi e organismi internazionali. </p>
									<p>
										<button type="submit" class="btn btn-default" onclick="location.href = '/cs/internet/istituto.html';">vai alla sezione
										</button>
									</p>
								</div>
								<ul role="menu">
									<li><a href="https://www.inail.it/cs/internet/istituto/chi-siamo.html">Chi siamo</a>
									</li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/struttura-organizzativa.html">Struttura
											organizzativa</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/territorio.html">Territorio</a>
									</li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/amministrazione-trasparente.html">Amministrazione
											trasparente</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/patrimonio-immobiliare/edifici-di-interesse-storico-artistico-e-architettonico.html">Edifici
											di interesse storico artistico e architettonico</a></li>
									<li><a href="https://www.inail.it/cs/internet/istituto/progetti.html">Progetti</a>
									</li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/relazioni-internazionali.html">Relazioni
											internazionali</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/organismo-notificato-0100.html">Organismo
											notificato 0100</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/sistema-nazionale-per-la-prevenzione.html">Sistema
											nazionale per la prevenzione </a></li>
									<li><a href="https://www.inail.it/cs/internet/istituto/access-point-eessi.html">Access
											Point EESSI</a></li>
									<li><a href="https://www.inail.it/cs/internet/istituto/focal-point-italia.html">Focal
											Point Italia</a></li>
									<li><a href="https://www.inail.it/cs/internet/istituto/innovazione-digitale.html">Innovazione
											digitale</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/contrasto-discriminazioni-e-benessere-lavorativo.html">Comitato
											unico di garanzia - Cug </a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/provider-crediti-formativi-ecm.html">Provider
											Inail Ecm</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/istituto/fatturazione-elettronica.html">Fatturazione
											elettronica</a></li>
								</ul>
							</div>
						</li>
						<li class="dropdown nav-item"><a href="https://www.inail.it/cs/internet/attivita.html" class="dropdown-toggle nav-link" data-toggle="dropdown" role="button" aria-expanded="false"> Attività <span
										class="sr-only">(selezionato)</span> </a>
							<div class="dropdown-menu">
								<div class="visible-sm-block visible-md-block visible-lg-block">
									<h3>Attività</h3>
									<p> Principali aree di azione volte a ridurre il fenomeno infortunistico, assicurare dai rischi e garantire la tutela dei lavoratori. </p>
									<p>
										<button type="submit" class="btn btn-default" onclick="location.href = '/cs/internet/attivita.html';">vai alla sezione
										</button>
									</p>
								</div>
								<ul role="menu">
									<li>
										<a href="https://www.inail.it/cs/internet/attivita/prevenzione-e-sicurezza.html">Prevenzione
											e sicurezza</a></li>
									<li><a href="https://www.inail.it/cs/internet/attivita/assicurazione.html">Assicurazione</a>
									</li>
									<li>
										<a href="https://www.inail.it/cs/internet/attivita/prestazioni.html">Prestazioni</a>
									</li>
									<li><a href="https://www.inail.it/cs/internet/attivita/ricerca-e-tecnologia.html">Ricerca
											e Tecnologia</a></li>
									<li><a href="https://www.inail.it/cs/internet/attivita/dati-e-statistiche.html">Dati
											e statistiche</a></li>
								</ul>
							</div>
						</li>
						<li class="dropdown nav-item"><a href="https://www.inail.it/cs/internet/atti-e-documenti.html" class="dropdown-toggle nav-link" data-toggle="dropdown" role="button" aria-expanded="false"> Atti e documenti <span
										class="sr-only">(selezionato)</span> </a>
							<div class="dropdown-menu">
								<div class="visible-sm-block visible-md-block visible-lg-block">
									<h3>Atti e
										documenti</h3>
									<p> Documentazione prodotta dagli Organi dell’Inail, protocolli d’intesa, convenzioni e accordi con enti e istituzioni, istruzioni operative e modulistica. </p>
									<p>
										<button type="submit" class="btn btn-default" onclick="location.href = '/cs/internet/atti-e-documenti.html';">vai alla sezione
										</button>
									</p>
								</div>
								<ul role="menu">
									<li>
										<a href="https://www.inail.it/cs/internet/atti-e-documenti/moduli-e-modelli.html">Moduli
											e modelli</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/atti-e-documenti/istruzioni-operative.html">Istruzioni
											operative</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/atti-e-documenti/note-e-provvedimenti.html">Note
											e provvedimenti</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/atti-e-documenti/protocolli-e-accordi.html">Protocolli
											e accordi</a></li>
								</ul>
							</div>
						</li>
						<li class="dropdown nav-item"><a href="https://www.inail.it/cs/internet/comunicazione.html" class="dropdown-toggle nav-link" data-toggle="dropdown" role="button" aria-expanded="false"> Comunicazione <span
										class="sr-only">(selezionato)</span> </a>
							<div class="dropdown-menu">
								<div class="visible-sm-block visible-md-block visible-lg-block">
									<h3>Comunicazione</h3>
									<p> Contenuti per i media, documenti e informazioni istituzionali utili ad approfondire attività, finalità e conoscenza dell’Istituto </p>
									<p>
										<button type="submit" class="btn btn-default" onclick="location.href = '/cs/internet/comunicazione.html';">vai alla sezione
										</button>
									</p>
								</div>
								<ul role="menu">
									<li><a href="https://www.inail.it/cs/internet/comunicazione/avvisi-e-scadenze.html">Avvisi
											e scadenze</a></li>
									<li><a href="https://www.inail.it/cs/internet/comunicazione/news-ed-eventi.html">News
											ed eventi</a></li>
									<li><a href="https://www.inail.it/cs/internet/comunicazione/sala-stampa.html">Sala
											Stampa</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/comunicazione/campagne.html">Campagne</a>
									</li>
									<li><a href="https://www.inail.it/cs/internet/comunicazione/pubblicazioni.html">Pubblicazioni</a>
									</li>
									<li><a href="https://www.inail.it/cs/internet/comunicazione/multimedia.html">Multimedia</a>
									</li>
									<li><a href="https://www.inail.it/cs/internet/comunicazione/social.html">Social</a>
									</li>
								</ul>
							</div>
						</li>
						<li class="dropdown nav-item"><a href="https://www.inail.it/cs/internet/servizi-per-te.html" class="dropdown-toggle nav-link" data-toggle="dropdown" role="button" aria-expanded="false"> Servizi Per Te <span
										class="sr-only">(selezionato)</span> </a>
							<div class="dropdown-menu">
								<div class="visible-sm-block visible-md-block visible-lg-block">
									<h3>Servizi Per Te</h3>
									<p> Strumenti e servizi per lavoratori e aziende </p>
									<p>
										<button type="submit" class="btn btn-default" onclick="location.href = '/cs/internet/servizi-per-te.html';">vai alla sezione
										</button>
									</p>
								</div>
								<ul role="menu">
									<li><a href="https://www.inail.it/cs/internet/servizi-per-te/lavoratore.html">Lavoratore</a>
									</li>
									<li><a href="https://www.inail.it/cs/internet/servizi-per-te/datore-di-lavoro.html">Datore
											di Lavoro</a></li>
									<li><a href="https://www.inail.it/cs/internet/servizi-per-te/consulente.html">Consulente</a>
									</li>
									<li><a href="https://www.inail.it/cs/internet/servizi-per-te/caf-e-patronati.html">Patronati
											e Caf</a></li>
									<li>
										<a href="https://www.inail.it/cs/internet/servizi-per-te/operatori-della-sanita.html">Operatori
											della sanità</a></li>
									<li><a href="https://www.inail.it/cs/internet/servizi-per-te/altri-utenti.html">Altri
											Utenti</a></li>
								</ul>
							</div>
						</li>
						<li class="dropdown nav-item"><a href="https://www.inail.it/cs/internet/supporto.html" class="dropdown-toggle nav-link" data-toggle="dropdown" role="button" aria-expanded="false"> Supporto <span
										class="sr-only">(selezionato)</span> </a>
							<div class="dropdown-menu">
								<div class="visible-sm-block visible-md-block visible-lg-block">
									<h3>Supporto</h3>
									<p> Servizi e sistemi a supporto delle attività istituzionali. </p>
									<p>
										<button type="submit" class="btn btn-default" onclick="location.href = 'https://profilazioneutenteinternet.inail.it/utilityredirect?contesto=SUPPORTO';">
											vai alla sezione
										</button>
									</p>
								</div>
								<ul role="menu">
									<li><a href="https://www.inail.it/cs/internet/supporto/guide-e-manuali.html">Guide e
											manuali operativi</a></li>
									<li><a href="https://www.inail.it/cs/internet/supporto/faq-sn.html">Faq</a></li>
									<li><a href="https://www.inail.it/cs/internet/supporto/inail-risponde.html">Inail
											risponde</a></li>
									<li><a href="https://www.inail.it/cs/internet/supporto/agenda-appuntamenti.html">Agenda
											appuntamenti</a></li>
									<li><a href="https://www.inail.it/cs/internet/supporto/sedi.html">Sedi</a></li>
								</ul>
							</div>
						</li>
                        <#if (Session.currentUser.username !="guest" )>
                        <@wp.ifauthorized permission="enterBackend">
							<li class="dropdown nav-item">
								<a href="<@wp.info key=" systemParam " paramName="applicationBaseURL " />do/main.action?request_locale=<@wp.info key="currentLang " />" class="btn dropdown-toggle nav-link">
                                    <@wp.i18n key="ADMINISTRATION" />
								</a>
							</li>
                        </@wp.ifauthorized>
							<li class="dropdown nav-item">
								<a href="<@wp.info key=" systemParam " paramName="applicationBaseURL " />do/logout.action" class="btn dropdown-toggle nav-link">
                                    <@wp.i18n key="LOGOUT" />
								</a>
							</li>
                        <#else>
							<script src="<@wp.info key=" systemParam " paramName="keycloakAuthUrl " />/js/keycloak.js"></script>
							<script>
                              var keycloak = Keycloak('<@wp.info key="systemParam" paramName="applicationBaseURL" />keycloak.json');
                              keycloak.init({
                                onLoad: 'check-sso'
                              }).success(function(authenticated) {
                                if (authenticated) {
                                  location.href = '<@wp.info key="systemParam" paramName="applicationBaseURL" />do/login?redirectTo=<@wp.url/>';
                                }
                              });
							</script>
							<ul role="menu" class="nav navbar-nav navbar-right">
								<li>
									<form action="<@wp.info key=" systemParam " paramName="applicationBaseURL " />do/login" method="get" class="form-horizontal margin-medium-top">
										<input type="hidden" name="redirectTo" value="<@wp.url/>" />
										<input id="service" type="submit" value="Accedi ai servizi online" class="nav-link" style="padding: 15px 80px 14px 80px; border: 0" />
									</form>
								</li>
							</ul>
                        </#if>
					</ul>
				</div>
			</div>
		</nav>
	</header>
</div>