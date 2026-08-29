package com.example.borderly

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

private data class ReferenceTranslation(
    val en: String,
    val es: String,
    val pt: String,
    val de: String,
    val fr: String
)

private val ReferenceTranslations = mapOf(
    "Некоммерческий слой исключён. Для территории публикуется только проверенная официальная политика, безопасное производное правило или статус отсутствия подтверждённых данных." to ReferenceTranslation(
        "The non-commercial layer is excluded. A territory only receives a verified official policy, a safe derived rule, or a no-confirmed-data status.", "Se excluye la capa no comercial. Para un territorio solo se publica una política oficial verificada, una regla derivada segura o el estado sin datos confirmados.", "A camada não comercial é excluída. Para um território, só é publicada uma política oficial verificada, uma regra derivada segura ou o status sem dados confirmados.", "Die nicht kommerzielle Ebene ist ausgeschlossen. Für ein Gebiet wird nur eine verifizierte offizielle Regel, eine sichere abgeleitete Regel oder der Status ohne bestätigte Daten veröffentlicht.", "La couche non commerciale est exclue. Pour un territoire, seule une règle officielle vérifiée, une règle dérivée sûre ou l’absence de données confirmées est publiée."
    ),
    "Основной открытый набор визовых статусов и сроков пребывания. Для обычных направлений Borderly синхронизирует категорию и срок 1:1 с зафиксированным снимком источника; явно зарегистрированные официальные правила имеют приоритет." to ReferenceTranslation(
        "Primary open dataset of visa statuses and stay lengths. For standard destinations, Borderly mirrors the recorded source snapshot; registered official rules take priority.", "Conjunto abierto principal de estados de visa y estadías. Para destinos estándar, Borderly replica la copia registrada de la fuente; las reglas oficiales tienen prioridad.", "Conjunto aberto principal de status de visto e períodos de estadia. Para destinos padrão, o Borderly replica o registro da fonte; regras oficiais têm prioridade.", "Primärer offener Datensatz zu Visastatus und Aufenthaltsdauer. Für Standardziele übernimmt Borderly den gespeicherten Quellenstand; registrierte offizielle Regeln haben Vorrang.", "Jeu de données ouvert principal sur les statuts de visa et les durées de séjour. Pour les destinations standard, Borderly reprend l’instantané enregistré ; les règles officielles sont prioritaires."
    ),
    "Реестр территориальных правил Borderly" to ReferenceTranslation(
        "Borderly territorial rules registry", "Registro de reglas territoriales de Borderly", "Registro de regras territoriais do Borderly", "Borderly-Register territorialer Regeln", "Registre des règles territoriales de Borderly"
    ),
    "DS-160 онлайн + собеседование" to ReferenceTranslation(
        "Online DS-160 + interview", "DS-160 en línea + entrevista", "DS-160 on-line + entrevista", "DS-160 online + Interview", "DS-160 en ligne + entretien"
    ),
    "Sri Lanka ETA / туристическое разрешение" to ReferenceTranslation(
        "Sri Lanka ETA / tourist authorization", "ETA de Sri Lanka / autorización turística", "ETA do Sri Lanka / autorização turística", "Sri Lanka ETA / Reisegenehmigung", "ETA Sri Lanka / autorisation touristique"
    ),
    "Безвизовый въезд" to ReferenceTranslation(
        "Visa-free entry", "Entrada sin visa", "Entrada sem visto", "Visumfreie Einreise", "Entrée sans visa"
    ),
    "Безвизовый въезд при подходящих условиях или онлайн-виза для отдельных случаев" to ReferenceTranslation(
        "Visa-free entry when eligible, or an online visa in certain cases", "Entrada sin visa si se cumplen los requisitos o visa en línea en ciertos casos", "Entrada sem visto quando elegível ou visto on-line em alguns casos", "Visumfreie Einreise bei Erfüllung der Bedingungen oder Online-Visum in bestimmten Fällen", "Entrée sans visa sous conditions ou visa en ligne dans certains cas"
    ),
    "Безвизовый туристический въезд" to ReferenceTranslation(
        "Visa-free tourist entry", "Entrada turística sin visa", "Entrada turística sem visto", "Visumfreie touristische Einreise", "Entrée touristique sans visa"
    ),
    "Бронь проживания или адрес пребывания" to ReferenceTranslation(
        "Accommodation booking or address", "Reserva de alojamiento o domicilio", "Reserva de hospedagem ou endereço", "Unterkunftsbuchung oder Aufenthaltsadresse", "Réservation d’hébergement ou adresse de séjour"
    ),
    "Виза по прибытии / краткосрочный въезд" to ReferenceTranslation(
        "Visa on arrival / short-term entry", "Visa al llegar / entrada de corta duración", "Visto na chegada / entrada de curta duração", "Visum bei Ankunft / Kurzaufenthalt", "Visa à l’arrivée / court séjour"
    ),
    "Виза по прибытии или e-Visa при подходящих условиях" to ReferenceTranslation(
        "Visa on arrival or eVisa when eligible", "Visa al llegar o eVisa si se cumplen los requisitos", "Visto na chegada ou eVisa quando elegível", "Visum bei Ankunft oder eVisa bei Erfüllung der Bedingungen", "Visa à l’arrivée ou eVisa sous conditions"
    ),
    "Въезд без предварительной визы по действующему документу" to ReferenceTranslation(
        "Entry with a valid document and no advance visa", "Entrada con documento vigente y sin visa previa", "Entrada com documento válido e sem visto prévio", "Einreise mit gültigem Dokument ohne vorheriges Visum", "Entrée avec un document valide sans visa préalable"
    ),
    "Въезд без предварительной визы по заграничному паспорту" to ReferenceTranslation(
        "Entry with a passport and no advance visa", "Entrada con pasaporte y sin visa previa", "Entrada com passaporte e sem visto prévio", "Einreise mit Reisepass ohne vorheriges Visum", "Entrée avec un passeport sans visa préalable"
    ),
    "Въезд по заграничному паспорту без предварительной визы" to ReferenceTranslation(
        "Entry with a passport without an advance visa", "Entrada con pasaporte sin visa previa", "Entrada com passaporte sem visto prévio", "Einreise mit Reisepass ohne vorheriges Visum", "Entrée avec un passeport sans visa préalable"
    ),
    "Дождитесь решения и сохраните подтверждение разрешения на въезд." to ReferenceTranslation(
        "Wait for the decision and save the entry authorization confirmation.", "Espera la decisión y guarda la confirmación de la autorización de entrada.", "Aguarde a decisão e salve a confirmação da autorização de entrada.", "Warten Sie auf die Entscheidung und speichern Sie die Einreisebestätigung.", "Attendez la décision et conservez la confirmation de l’autorisation d’entrée."
    ),
    "Египетская туристическая виза" to ReferenceTranslation(
        "Egypt tourist visa", "Visa turística de Egipto", "Visto de turista do Egito", "Touristenvisum für Ägypten", "Visa touristique pour l’Égypte"
    ),
    "Заграничный паспорт" to ReferenceTranslation(
        "Passport", "Pasaporte", "Passaporte", "Reisepass", "Passeport"
    ),
    "Заполните анкету на официальном сайте или подготовьте пакет для визового центра/консульства." to ReferenceTranslation(
        "Complete the form on the official site or prepare the documents for the visa center or consulate.", "Completa el formulario en el sitio oficial o prepara los documentos para el centro de visas o consulado.", "Preencha o formulário no site oficial ou prepare os documentos para o centro de vistos ou consulado.", "Füllen Sie das Formular auf der offiziellen Website aus oder bereiten Sie die Unterlagen für Visazentrum oder Konsulat vor.", "Remplissez le formulaire sur le site officiel ou préparez le dossier pour le centre des visas ou le consulat."
    ),
    "Кипрская краткосрочная виза" to ReferenceTranslation(
        "Cyprus short-stay visa", "Visa de corta duración de Chipre", "Visto de curta duração do Chipre", "Kurzaufenthaltsvisum für Zypern", "Visa de court séjour pour Chypre"
    ),
    "Китайская туристическая виза L" to ReferenceTranslation(
        "China tourist visa L", "Visa turística L de China", "Visto de turista L da China", "Chinesisches Touristenvisum L", "Visa touristique chinois L"
    ),
    "Краткосрочная виза / K-ETA при применимости" to ReferenceTranslation(
        "Short-stay visa / K-ETA when applicable", "Visa de corta duración / K-ETA cuando corresponda", "Visto de curta duração / K-ETA quando aplicável", "Kurzaufenthaltsvisum / K-ETA, falls anwendbar", "Visa de court séjour / K-ETA le cas échéant"
    ),
    "Краткосрочная виза Японии" to ReferenceTranslation(
        "Japan short-stay visa", "Visa de corta duración de Japón", "Visto de curta duração do Japão", "Japanisches Kurzaufenthaltsvisum", "Visa japonais de court séjour"
    ),
    "Краткосрочный туристический въезд / Thai e-Visa" to ReferenceTranslation(
        "Short tourist stay / Thai eVisa", "Estadía turística corta / eVisa de Tailandia", "Estadia turística curta / eVisa da Tailândia", "Touristischer Kurzaufenthalt / thailändisches eVisa", "Court séjour touristique / eVisa thaïlandais"
    ),
    "Медицинская страховка, если требуется" to ReferenceTranslation(
        "Travel health insurance, if required", "Seguro médico, si se requiere", "Seguro-saúde, se exigido", "Reisekrankenversicherung, falls erforderlich", "Assurance santé, si nécessaire"
    ),
    "Обратные билеты или маршрут поездки" to ReferenceTranslation(
        "Return tickets or travel itinerary", "Boletos de regreso o itinerario", "Passagens de volta ou itinerário", "Rückflugtickets oder Reiseplan", "Billets retour ou itinéraire"
    ),
    "Онлайн-анкета GOV.UK + биометрия" to ReferenceTranslation(
        "GOV.UK online form + biometrics", "Formulario en línea de GOV.UK + biometría", "Formulário on-line GOV.UK + biometria", "GOV.UK-Onlineformular + biometrische Daten", "Formulaire GOV.UK en ligne + biométrie"
    ),
    "Онлайн-оформление электронного разрешения" to ReferenceTranslation(
        "Online electronic authorization", "Autorización electrónica en línea", "Autorização eletrônica on-line", "Elektronische Genehmigung online", "Autorisation électronique en ligne"
    ),
    "Онлайн-подача eVISA при подходящих условиях или консульская подача" to ReferenceTranslation(
        "Online eVisa application when eligible, or a consular application", "Solicitud de eVisa en línea si corresponde o solicitud consular", "Solicitação de eVisa on-line quando elegível ou solicitação consular", "Online-eVisa-Antrag bei Erfüllung der Bedingungen oder Antrag beim Konsulat", "Demande d’eVisa en ligne sous conditions ou demande consulaire"
    ),
    "Онлайн-подача через IRCC" to ReferenceTranslation(
        "Online application through IRCC", "Solicitud en línea mediante IRCC", "Solicitação on-line pelo IRCC", "Online-Antrag über IRCC", "Demande en ligne via IRCC"
    ),
    "Онлайн-подача через ImmiAccount" to ReferenceTranslation(
        "Online application through ImmiAccount", "Solicitud en línea mediante ImmiAccount", "Solicitação on-line pelo ImmiAccount", "Online-Antrag über ImmiAccount", "Demande en ligne via ImmiAccount"
    ),
    "Онлайн-подача через Indian e-Visa" to ReferenceTranslation(
        "Online application through Indian e-Visa", "Solicitud en línea mediante Indian e-Visa", "Solicitação on-line pelo Indian e-Visa", "Online-Antrag über Indian e-Visa", "Demande en ligne via Indian e-Visa"
    ),
    "Онлайн-подача через туристический визовый сервис" to ReferenceTranslation(
        "Online application through the tourist visa service", "Solicitud en línea mediante el servicio de visas turísticas", "Solicitação on-line pelo serviço de vistos turísticos", "Online-Antrag über den Touristenvisum-Service", "Demande en ligne via le service des visas touristiques"
    ),
    "Оплатите сбор, если он предусмотрен." to ReferenceTranslation(
        "Pay the fee, if applicable.", "Paga la tarifa, si corresponde.", "Pague a taxa, se aplicável.", "Zahlen Sie die Gebühr, falls vorgesehen.", "Payez les frais, le cas échéant."
    ),
    "Оформление при прибытии по правилам ОАЭ" to ReferenceTranslation(
        "Processing on arrival under UAE rules", "Trámite al llegar según las reglas de EAU", "Emissão na chegada conforme as regras dos EAU", "Abwicklung bei Ankunft nach den Regeln der VAE", "Formalités à l’arrivée selon les règles des EAU"
    ),
    "Оформление при прибытии при выполнении требований" to ReferenceTranslation(
        "Processing on arrival when requirements are met", "Trámite al llegar si se cumplen los requisitos", "Emissão na chegada quando os requisitos forem atendidos", "Abwicklung bei Ankunft bei Erfüllung der Bedingungen", "Formalités à l’arrivée si les conditions sont remplies"
    ),
    "Оформление туристической карты через перевозчика, туроператора или консульский канал" to ReferenceTranslation(
        "Tourist card through the carrier, tour operator, or consular channel", "Tarjeta de turista mediante la aerolínea, operador turístico o canal consular", "Cartão de turista pela transportadora, operadora ou via consular", "Touristenkarte über Beförderer, Reiseveranstalter oder Konsulat", "Carte de tourisme via le transporteur, le voyagiste ou le consulat"
    ),
    "Перед вылетом проверьте срок действия паспорта, обратные билеты и требования авиакомпании." to ReferenceTranslation(
        "Before departure, check passport validity, return tickets, and airline requirements.", "Antes de salir, verifica la vigencia del pasaporte, los boletos de regreso y los requisitos de la aerolínea.", "Antes do embarque, confira a validade do passaporte, as passagens de volta e as exigências da companhia aérea.", "Prüfen Sie vor Abflug Passgültigkeit, Rückflugticket und Vorgaben der Fluggesellschaft.", "Avant le départ, vérifiez la validité du passeport, les billets retour et les exigences de la compagnie aérienne."
    ),
    "Подача через France-Visas" to ReferenceTranslation(
        "Application through France-Visas", "Solicitud mediante France-Visas", "Solicitação pelo France-Visas", "Antrag über France-Visas", "Demande via France-Visas"
    ),
    "Подача через визовый центр или консульский канал" to ReferenceTranslation(
        "Application through a visa center or consular channel", "Solicitud mediante un centro de visas o canal consular", "Solicitação por centro de vistos ou via consular", "Antrag über Visazentrum oder Konsulat", "Demande via un centre des visas ou le consulat"
    ),
    "Подача через визовый центр/консульство Германии" to ReferenceTranslation(
        "Application through a German visa center or consulate", "Solicitud mediante un centro de visas o consulado de Alemania", "Solicitação por centro de vistos ou consulado da Alemanha", "Antrag über deutsches Visazentrum oder Konsulat", "Demande via un centre des visas ou consulat allemand"
    ),
    "Подача через визовый центр/консульство Греции" to ReferenceTranslation(
        "Application through a Greek visa center or consulate", "Solicitud mediante un centro de visas o consulado de Grecia", "Solicitação por centro de vistos ou consulado da Grécia", "Antrag über griechisches Visazentrum oder Konsulat", "Demande via un centre des visas ou consulat grec"
    ),
    "Подача через визовый центр/консульство Испании" to ReferenceTranslation(
        "Application through a Spanish visa center or consulate", "Solicitud mediante un centro de visas o consulado de España", "Solicitação por centro de vistos ou consulado da Espanha", "Antrag über spanisches Visazentrum oder Konsulat", "Demande via un centre des visas ou consulat espagnol"
    ),
    "Подача через визовый центр/консульство Италии" to ReferenceTranslation(
        "Application through an Italian visa center or consulate", "Solicitud mediante un centro de visas o consulado de Italia", "Solicitação por centro de vistos ou consulado da Itália", "Antrag über italienisches Visazentrum oder Konsulat", "Demande via un centre des visas ou consulat italien"
    ),
    "Подача через консульский канал или визовый центр" to ReferenceTranslation(
        "Application through a consular channel or visa center", "Solicitud mediante un canal consular o centro de visas", "Solicitação por via consular ou centro de vistos", "Antrag über Konsulat oder Visazentrum", "Demande via le consulat ou un centre des visas"
    ),
    "Подача через консульство или визовый центр, если применимо" to ReferenceTranslation(
        "Application through a consulate or visa center, if applicable", "Solicitud mediante consulado o centro de visas, si corresponde", "Solicitação por consulado ou centro de vistos, se aplicável", "Antrag über Konsulat oder Visazentrum, falls zutreffend", "Demande via le consulat ou un centre des visas, le cas échéant"
    ),
    "Подготовьте бронь проживания, обратный билет и страховку, если их может запросить пограничник." to ReferenceTranslation(
        "Prepare accommodation booking, a return ticket, and insurance if border officers may request them.", "Prepara la reserva de alojamiento, el boleto de regreso y el seguro si pueden solicitarlos en frontera.", "Prepare reserva de hospedagem, passagem de volta e seguro caso sejam solicitados na fronteira.", "Bereiten Sie Unterkunftsbuchung, Rückflugticket und Versicherung vor, falls die Grenzbehörde sie verlangt.", "Préparez la réservation d’hébergement, le billet retour et l’assurance s’ils peuvent être demandés à la frontière."
    ),
    "Подтверждение финансовых средств" to ReferenceTranslation(
        "Proof of sufficient funds", "Comprobante de fondos suficientes", "Comprovante de recursos financeiros", "Nachweis ausreichender finanzieller Mittel", "Justificatif de ressources financières"
    ),
    "Пошлина зависит от типа разрешения и способа оформления." to ReferenceTranslation(
        "The fee depends on the authorization type and application method.", "La tarifa depende del tipo de autorización y del método de solicitud.", "A taxa depende do tipo de autorização e da forma de solicitação.", "Die Gebühr hängt von Genehmigungsart und Antragsweg ab.", "Les frais dépendent du type d’autorisation et du mode de demande."
    ),
    "Правила въезда могут меняться. Проверяйте условия незадолго до поездки." to ReferenceTranslation(
        "Entry rules may change. Recheck the requirements shortly before travel.", "Las reglas de entrada pueden cambiar. Verifica los requisitos poco antes de viajar.", "As regras de entrada podem mudar. Confira os requisitos pouco antes da viagem.", "Einreiseregeln können sich ändern. Prüfen Sie die Bedingungen kurz vor der Reise erneut.", "Les règles d’entrée peuvent changer. Vérifiez les conditions peu avant le voyage."
    ),
    "Проверка права на K-ETA или оформление визы" to ReferenceTranslation(
        "K-ETA eligibility check or visa application", "Verificación de elegibilidad para K-ETA o solicitud de visa", "Verificação de elegibilidade para K-ETA ou solicitação de visto", "Prüfung der K-ETA-Berechtigung oder Visumantrag", "Vérification de l’éligibilité au K-ETA ou demande de visa"
    ),
    "Проверьте официальные правила и требования авиакомпании перед вылетом." to ReferenceTranslation(
        "Check official rules and airline requirements before departure.", "Consulta las reglas oficiales y los requisitos de la aerolínea antes de salir.", "Confira as regras oficiais e as exigências da companhia aérea antes do embarque.", "Prüfen Sie vor Abflug die offiziellen Regeln und Vorgaben der Fluggesellschaft.", "Vérifiez les règles officielles et les exigences de la compagnie aérienne avant le départ."
    ),
    "Проверьте разрешенный срок пребывания для граждан России." to ReferenceTranslation(
        "Check the permitted stay for Russian citizens.", "Consulta la estadía permitida para ciudadanos de Rusia.", "Confira o período permitido para cidadãos da Rússia.", "Prüfen Sie die zulässige Aufenthaltsdauer für russische Staatsangehörige.", "Vérifiez la durée de séjour autorisée pour les ressortissants russes."
    ),
    "Проверьте, подходит ли ваша цель поездки под туристический или краткосрочный формат." to ReferenceTranslation(
        "Check whether your purpose of travel qualifies as tourism or a short stay.", "Verifica si el motivo del viaje corresponde a turismo o corta duración.", "Confira se o motivo da viagem se enquadra em turismo ou curta duração.", "Prüfen Sie, ob Ihr Reisezweck als Tourismus oder Kurzaufenthalt gilt.", "Vérifiez si le motif du voyage relève du tourisme ou d’un court séjour."
    ),
    "Сроки обработки зависят от сезона, анкеты и выбранного способа подачи." to ReferenceTranslation(
        "Processing times depend on the season, application, and submission method.", "Los plazos dependen de la temporada, la solicitud y el método de presentación.", "Os prazos dependem da temporada, do formulário e da forma de solicitação.", "Die Bearbeitungszeit hängt von Saison, Antrag und Antragsweg ab.", "Les délais dépendent de la saison, du dossier et du mode de demande."
    ),
    "Точный список документов зависит от цели поездки, срока пребывания и требований перевозчика. Перед оплатой билетов проверьте официальный источник." to ReferenceTranslation(
        "The exact documents depend on the trip purpose, stay length, and carrier requirements. Check the official source before paying for tickets.", "Los documentos exactos dependen del motivo, la duración y la aerolínea. Consulta la fuente oficial antes de pagar los boletos.", "Os documentos exatos dependem do motivo, da duração e da transportadora. Confira a fonte oficial antes de pagar as passagens.", "Die genauen Unterlagen hängen von Reisezweck, Aufenthaltsdauer und Beförderer ab. Prüfen Sie vor dem Ticketkauf die offizielle Quelle.", "Les documents exacts dépendent du motif, de la durée et du transporteur. Vérifiez la source officielle avant de payer les billets."
    ),
    "Туристическая виза по прибытии" to ReferenceTranslation(
        "Tourist visa on arrival", "Visa turística al llegar", "Visto de turista na chegada", "Touristenvisum bei Ankunft", "Visa touristique à l’arrivée"
    ),
    "Туристическая карта Кубы" to ReferenceTranslation(
        "Cuba tourist card", "Tarjeta de turista de Cuba", "Cartão de turista de Cuba", "Touristenkarte für Kuba", "Carte de tourisme pour Cuba"
    ),
    "Убедитесь, что паспорт действует достаточно долго для въезда." to ReferenceTranslation(
        "Make sure the passport remains valid long enough for entry.", "Asegúrate de que el pasaporte tenga vigencia suficiente para entrar.", "Confira se o passaporte tem validade suficiente para a entrada.", "Stellen Sie sicher, dass der Reisepass für die Einreise lange genug gültig ist.", "Assurez-vous que le passeport reste valable assez longtemps pour l’entrée."
    ),
    "Фото или цифровая фотография, если требуется" to ReferenceTranslation(
        "Photo or digital photo, if required", "Foto física o digital, si se requiere", "Foto impressa ou digital, se exigida", "Passfoto oder digitales Foto, falls erforderlich", "Photo papier ou numérique, si nécessaire"
    ),
    "Шенгенская краткосрочная виза C" to ReferenceTranslation(
        "Schengen short-stay visa C", "Visa Schengen de corta duración C", "Visto Schengen de curta duração C", "Schengen-Kurzaufenthaltsvisum C", "Visa Schengen de court séjour C"
    ),
    "В течение 3 дней до прибытия" to ReferenceTranslation(
        "Within 3 days before arrival", "Dentro de los 3 días previos a la llegada", "Nos 3 dias anteriores à chegada", "Innerhalb von 3 Tagen vor der Ankunft", "Dans les 3 jours précédant l’arrivée"
    ),
    "Для безвизового въезда подайте электронное заявление через ruID. Это дополнительная процедура безвизового въезда, а не eTA/ESTA и не электронная виза." to ReferenceTranslation(
        "For visa-free entry, submit an electronic application through ruID. This is an additional visa-free entry procedure, not an eTA/ESTA or eVisa.", "Para entrar sin visa, presenta una solicitud electrónica mediante ruID. Es un trámite adicional, no una eTA/ESTA ni una eVisa.", "Para entrar sem visto, envie uma solicitação eletrônica pelo ruID. É um procedimento adicional, não eTA/ESTA nem eVisa.", "Für die visumfreie Einreise ist ein elektronischer Antrag über ruID nötig. Dies ist ein zusätzliches Verfahren, keine eTA/ESTA oder eVisa.", "Pour l’entrée sans visa, déposez une demande électronique via ruID. Il s’agit d’une formalité supplémentaire, et non d’une eTA/ESTA ou d’un eVisa."
    ),
    "Для въезда нужно предоставить данные Arrival Card. Форму можно заполнить онлайн заранее либо оформить по прибытии через доступные официальные каналы." to ReferenceTranslation(
        "Arrival Card details are required for entry. Complete the form online in advance or on arrival through an available official channel.", "Para entrar debes proporcionar los datos de la Arrival Card. Complétala en línea antes del viaje o al llegar por un canal oficial disponible.", "Para entrar, informe os dados do Arrival Card. Preencha on-line antes da viagem ou na chegada por um canal oficial disponível.", "Für die Einreise sind Angaben der Arrival Card erforderlich. Füllen Sie sie vorab online oder bei Ankunft über einen offiziellen Kanal aus.", "Les informations de l’Arrival Card sont requises. Remplissez-la en ligne à l’avance ou à l’arrivée via un canal officiel disponible."
    ),
    "До поездки онлайн или по прибытии" to ReferenceTranslation(
        "Online before travel or on arrival", "En línea antes del viaje o al llegar", "On-line antes da viagem ou na chegada", "Online vor der Reise oder bei Ankunft", "En ligne avant le voyage ou à l’arrivée"
    ),
    "Если не заполняли заранее, предоставьте данные по прибытии через официальный QR-код, терминал или бумажную форму, когда это доступно." to ReferenceTranslation(
        "If not completed in advance, provide the details on arrival using the official QR code, terminal, or paper form where available.", "Si no la completaste antes, proporciona los datos al llegar mediante el QR oficial, una terminal o un formulario en papel, cuando estén disponibles.", "Se não preencher antes, informe os dados na chegada pelo QR oficial, terminal ou formulário em papel, quando disponíveis.", "Falls nicht vorab erledigt, geben Sie die Daten bei Ankunft über offiziellen QR-Code, Terminal oder Papierformular an, sofern verfügbar.", "Si vous ne l’avez pas remplie à l’avance, fournissez les données à l’arrivée via le QR code officiel, une borne ou un formulaire papier, selon disponibilité."
    ),
    "Заполните MDAC на официальном сайте Иммиграционной службы Малайзии." to ReferenceTranslation(
        "Complete the MDAC on the official Malaysian Immigration website.", "Completa la MDAC en el sitio oficial de Inmigración de Malasia.", "Preencha o MDAC no site oficial da Imigração da Malásia.", "Füllen Sie die MDAC auf der offiziellen Website der malaysischen Einwanderungsbehörde aus.", "Remplissez la MDAC sur le site officiel de l’immigration malaisienne."
    ),
    "Заполните заявление о планируемом въезде через официальный сервис ruID." to ReferenceTranslation(
        "Complete the planned-entry application through the official ruID service.", "Completa la solicitud de entrada prevista mediante el servicio oficial ruID.", "Preencha a solicitação de entrada planejada pelo serviço oficial ruID.", "Füllen Sie den Antrag zur geplanten Einreise über den offiziellen ruID-Dienst aus.", "Remplissez la demande d’entrée prévue via le service officiel ruID."
    ),
    "Заявление о въезде через ruID" to ReferenceTranslation(
        "Entry application through ruID", "Solicitud de entrada mediante ruID", "Solicitação de entrada pelo ruID", "Einreiseantrag über ruID", "Demande d’entrée via ruID"
    ),
    "Обычно не позже чем за 72 часа до въезда; при срочном въезде — не позже чем за 4 часа" to ReferenceTranslation(
        "Usually no later than 72 hours before entry; for urgent entry, no later than 4 hours", "Por lo general, máximo 72 horas antes de entrar; en casos urgentes, máximo 4 horas antes", "Normalmente até 72 horas antes da entrada; em caso urgente, até 4 horas antes", "Normalerweise spätestens 72 Stunden vor Einreise; bei dringender Einreise spätestens 4 Stunden vorher", "En général au plus tard 72 heures avant l’entrée ; en urgence, au plus tard 4 heures avant"
    ),
    "Перед въездом зарегистрируйте Malaysia Digital Arrival Card (MDAC). Это отдельная въездная форма и не является визой или eTA." to ReferenceTranslation(
        "Register the Malaysia Digital Arrival Card (MDAC) before entry. It is a separate entry form, not a visa or eTA.", "Registra la Malaysia Digital Arrival Card (MDAC) antes de entrar. Es un formulario aparte, no una visa ni una eTA.", "Registre o Malaysia Digital Arrival Card (MDAC) antes da entrada. É um formulário separado, não visto nem eTA.", "Registrieren Sie vor der Einreise die Malaysia Digital Arrival Card (MDAC). Sie ist ein separates Einreiseformular, kein Visum und keine eTA.", "Enregistrez la Malaysia Digital Arrival Card (MDAC) avant l’entrée. C’est un formulaire distinct, pas un visa ni une eTA."
    ),
    "Подайте его не раньше чем за 90 дней и обычно не позже чем за 72 часа до въезда." to ReferenceTranslation(
        "Submit it no earlier than 90 days and usually no later than 72 hours before entry.", "Preséntala no más de 90 días antes y, por lo general, hasta 72 horas antes de entrar.", "Envie com no máximo 90 dias de antecedência e, em geral, até 72 horas antes da entrada.", "Reichen Sie ihn frühestens 90 Tage und normalerweise spätestens 72 Stunden vor der Einreise ein.", "Déposez-la au plus tôt 90 jours et généralement au plus tard 72 heures avant l’entrée."
    ),
    "При желании заполните Arrival Card заранее через официальный сервис NIA." to ReferenceTranslation(
        "Optionally complete the Arrival Card in advance through the official NIA service.", "Si quieres, completa la Arrival Card con anticipación mediante el servicio oficial de NIA.", "Se desejar, preencha o Arrival Card antecipadamente pelo serviço oficial da NIA.", "Füllen Sie die Arrival Card bei Bedarf vorab über den offiziellen NIA-Dienst aus.", "Vous pouvez remplir l’Arrival Card à l’avance via le service officiel de la NIA."
    ),
    "При срочном въезде применяется сокращённый срок подачи при наличии предусмотренного основания." to ReferenceTranslation(
        "For urgent entry, a shorter submission period applies when an eligible reason exists.", "Para entradas urgentes se aplica un plazo menor cuando existe un motivo válido.", "Para entrada urgente, aplica-se um prazo menor quando houver motivo previsto.", "Bei dringender Einreise gilt bei entsprechendem Grund eine verkürzte Antragsfrist.", "En cas d’entrée urgente, un délai réduit s’applique lorsqu’un motif admissible existe."
    ),
    "Сохраните подтверждение регистрации до прохождения пограничного контроля." to ReferenceTranslation(
        "Keep the registration confirmation until border control is completed.", "Guarda la confirmación del registro hasta pasar el control fronterizo.", "Guarde a confirmação do registro até concluir o controle de fronteira.", "Bewahren Sie die Registrierungsbestätigung bis nach der Grenzkontrolle auf.", "Conservez la confirmation d’enregistrement jusqu’au passage du contrôle frontalier."
    ),
    "Госуслуги / МВД России" to ReferenceTranslation(
        "Gosuslugi / Russian Ministry of Internal Affairs", "Gosuslugi / Ministerio del Interior de Rusia", "Gosuslugi / Ministério do Interior da Rússia", "Gosuslugi / russisches Innenministerium", "Gosuslugi / ministère russe de l’Intérieur"
    ),
    "Укажите данные паспорта и поездки." to ReferenceTranslation(
        "Enter the passport and trip details.", "Ingresa los datos del pasaporte y del viaje.", "Informe os dados do passaporte e da viagem.", "Geben Sie Reisepass- und Reisedaten an.", "Renseignez les informations du passeport et du voyage."
    )
)

@Composable
internal fun localizedReferenceText(text: String): String {
    val translation = ReferenceTranslations[text] ?: return text
    return when (LocalConfiguration.current.locales[0].language) {
        "en" -> translation.en
        "es" -> translation.es
        "pt" -> translation.pt
        "de" -> translation.de
        "fr" -> translation.fr
        else -> text
    }
}
