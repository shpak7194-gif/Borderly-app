package com.example.borderly


internal fun mapCountryInfo(
    countryIso: Int,
    name: String,
    flag: String,
    passport: Passport,
    requirements: Map<Int, VisaRequirement>,
    entryGuideDatabase: EntryGuideDatabase,
    entryRequirementDatabase: EntryRequirementDatabase
): CountryInfo {
    val requirement = requirements[countryIso]
    val visaType = visaTypeFor(passport, countryIso, requirements)
    val entryGuide = entryGuideDatabase.guideFor(
        passportIso = passport.isoNumeric,
        destinationIso = countryIso,
        currentVisaType = visaType
    )
    val entryRequirements = entryRequirementDatabase.requirementsFor(
        passportIso = passport.isoNumeric,
        destinationIso = countryIso,
        currentVisaType = visaType
    )
    val days = if (
        visaType == VisaType.HOME_COUNTRY ||
        visaType == VisaType.FREEDOM ||
        visaType == VisaType.ENTRY_RESTRICTED ||
        visaType == VisaType.SPECIAL_PERMIT ||
        visaType == VisaType.MIXED_REQUIREMENTS
    ) null else requirement?.stayDays

    val stay = when {
        visaType == VisaType.HOME_COUNTRY -> "Страна выбранного паспорта"
        visaType == VisaType.FREEDOM -> "Свобода передвижения"
        visaType == VisaType.ENTRY_RESTRICTED ->
            "Обычный туристический въезд ограничен"
        visaType == VisaType.SPECIAL_PERMIT -> "Требуется специальное разрешение"
        visaType == VisaType.MIXED_REQUIREMENTS -> "Условия въезда различаются"
        visaType == VisaType.VISA_FREE && days != null -> "Без визы до $days дней"
        visaType == VisaType.VISA_ON_ARRIVAL && days != null ->
            "Виза по прибытии · до $days дней"
        visaType == VisaType.E_VISA && days != null -> "Электронная виза · до $days дней"
        visaType == VisaType.ETA && days != null ->
            "eTA/ESTA · до $days дней"
        else -> when (visaType) {
            VisaType.HOME_COUNTRY -> "Страна выбранного паспорта"
            VisaType.FREEDOM -> "Свобода передвижения"
            VisaType.VISA_FREE -> "Безвизовый въезд"
            VisaType.ETA -> "eTA/ESTA до поездки"
            VisaType.VISA_ON_ARRIVAL -> "Виза оформляется по прибытии"
            VisaType.E_VISA -> "Электронная виза до поездки"
            VisaType.VISA_REQUIRED -> "Требуется предварительная виза"
            VisaType.ENTRY_RESTRICTED -> "Обычный туристический въезд ограничен"
            VisaType.SPECIAL_PERMIT -> "Требуется специальное разрешение"
            VisaType.MIXED_REQUIREMENTS -> "Условия зависят от территории или маршрута"
            VisaType.NO_DATA -> "Нет подтверждённых данных"
        }
    }

    val stayCondition = when {
        visaType == VisaType.HOME_COUNTRY -> "Без визовых ограничений"
        visaType == VisaType.FREEDOM -> "По специальному режиму"
        days != null -> "До $days дней"
        visaType == VisaType.VISA_REQUIRED -> "По условиям выданной визы"
        visaType == VisaType.ENTRY_RESTRICTED -> "Обычный туризм ограничен"
        visaType == VisaType.SPECIAL_PERMIT -> "По условиям специального разрешения"
        visaType == VisaType.MIXED_REQUIREMENTS -> "Зависит от территории или маршрута"
        visaType == VisaType.NO_DATA -> "Нет подтверждённых данных"
        else -> "Зависит от условий разрешения"
    }

    val preApproval = when (visaType) {
        VisaType.HOME_COUNTRY -> "Не требуется"
        VisaType.FREEDOM -> "Виза не требуется"
        VisaType.VISA_FREE -> "Виза не требуется"
        VisaType.ETA -> "Нужно оформить eTA/ESTA"
        VisaType.VISA_ON_ARRIVAL -> "Оформление по прибытии"
        VisaType.E_VISA -> "Нужно оформить электронную визу"
        VisaType.VISA_REQUIRED -> "Нужно получить визу"
        VisaType.ENTRY_RESTRICTED -> "Нужно подходящее основание для въезда"
        VisaType.SPECIAL_PERMIT -> "Нужно специальное разрешение"
        VisaType.MIXED_REQUIREMENTS -> "Нужно проверить конкретный маршрут"
        VisaType.NO_DATA -> "Уточните перед поездкой"
    }

    val entryConditions = buildList {
        add(EntryCondition("Срок пребывания", stayCondition))

        if (
            visaType == VisaType.ETA ||
            visaType == VisaType.E_VISA ||
            visaType == VisaType.VISA_REQUIRED ||
            visaType == VisaType.VISA_ON_ARRIVAL ||
            visaType == VisaType.ENTRY_RESTRICTED ||
            visaType == VisaType.SPECIAL_PERMIT ||
            visaType == VisaType.MIXED_REQUIREMENTS ||
            visaType == VisaType.NO_DATA
        ) {
            add(
                EntryCondition(
                    "До поездки",
                    preApproval,
                    accent = visaType != VisaType.NO_DATA
                )
            )
        }
    }

    val beforeTrip = buildList {
        when (visaType) {
            VisaType.HOME_COUNTRY -> Unit

            VisaType.FREEDOM -> {
                add("Проверьте местные правила регистрации и пребывания")
            }

            VisaType.VISA_FREE -> {
                add("Проверьте срок действия паспорта для этой поездки")
            }

            VisaType.ETA -> {
                add("Оформите требуемое eTA/ESTA до поездки")
                add("Проверьте срок действия паспорта для этой поездки")
            }

            VisaType.VISA_ON_ARRIVAL -> {
                add("Проверьте условия оформления и визовый сбор по прибытии")
                add("Проверьте срок действия паспорта для этой поездки")
            }

            VisaType.E_VISA -> {
                add("Оформите электронную визу до поездки")
                add("Проверьте срок действия паспорта для этой поездки")
            }

            VisaType.VISA_REQUIRED -> {
                add("Получите визу до поездки")
                add("Проверьте срок действия паспорта для этой поездки")
            }

            VisaType.ENTRY_RESTRICTED -> {
                add("Проверьте, относитесь ли вы к разрешённой категории въезда")
            }

            VisaType.SPECIAL_PERMIT -> {
                add("Получите специальное разрешение до поездки")
                add("Проверьте ограничения доступа в официальном источнике")
            }

            VisaType.MIXED_REQUIREMENTS -> {
                add("Проверьте правила именно для выбранной территории и маршрута")
            }

            VisaType.NO_DATA -> {
                add("Уточните визовый режим в официальном источнике")
            }
        }
    }

    val applicationDocumentsTitle = entryGuide?.let {
        "Документы для паспорта «${passport.name}»"
    }
    val applicationDocuments = entryGuide?.documents.orEmpty()
    val applicationDocumentsNote = entryGuide?.documentsNote


    val passportNote = when (visaType) {
        VisaType.HOME_COUNTRY ->
            "Для этого направления выбран паспорт самой страны."

        VisaType.FREEDOM ->
            "Для паспорта «${passport.name}» действует специальный режим свободного " +
                "передвижения. Правила регистрации, проживания и работы могут " +
                "регулироваться отдельно."

        VisaType.VISA_FREE ->
            if (days != null) {
                "С паспортом «${passport.name}» предварительная виза не нужна для " +
                    "поездки сроком до $days дней."
            } else {
                "С паспортом «${passport.name}» предварительная виза не требуется. " +
                    "Точный допустимый срок лучше проверить перед поездкой."
            }

        VisaType.ETA ->
            "С паспортом «${passport.name}» виза не требуется, но до поездки нужна " +
                "eTA/ESTA. Официальное название разрешения зависит от страны."

        VisaType.VISA_ON_ARRIVAL ->
            "С паспортом «${passport.name}» предварительную визу обычно оформлять " +
                "не нужно: разрешение выдаётся по прибытии при выполнении условий."

        VisaType.E_VISA ->
            "Для паспорта «${passport.name}» разрешение оформляется онлайн до поездки. " +
                "Электронная виза не является безвизовым въездом."

        VisaType.VISA_REQUIRED ->
            "Для паспорта «${passport.name}» требуется получить визу до поездки."

        VisaType.ENTRY_RESTRICTED ->
            "Для паспорта «${passport.name}» обычный туристический въезд ограничен. " +
                "Исключения могут зависеть от цели поездки, ВНЖ, родства, транзита " +
                "или гуманитарного основания."

        VisaType.SPECIAL_PERMIT ->
            "Для этого направления требуется отдельное специальное разрешение. " +
                "Оно не приравнивается к обычной визе или электронному разрешению на поездку."

        VisaType.MIXED_REQUIREMENTS ->
            "Для этого направления нельзя безопасно показать одну обычную визовую категорию: " +
                "условия зависят от территории, зоны контроля или маршрута въезда."

        VisaType.NO_DATA ->
            "Для паспорта «${passport.name}» в базе Borderly пока нет подтверждённого " +
                "визового статуса по этому направлению."
    }

    val statusExplanation = when (visaType) {
        VisaType.HOME_COUNTRY ->
            "Это страна выбранного паспорта, поэтому обычная визовая классификация не применяется."

        VisaType.FREEDOM ->
            "Статус выделен отдельно от обычного «без визы»: для этой пары стран действует " +
                "расширенный режим мобильности."

        VisaType.VISA_FREE ->
            "Предварительная виза для обычной краткосрочной поездки не требуется."

        VisaType.ETA ->
            "Предварительная виза не нужна, но въезд зависит от электронной авторизации, " +
                "которую получают до поездки."

        VisaType.VISA_ON_ARRIVAL ->
            "Визовое разрешение оформляется после прибытия, а не заранее в консульстве."

        VisaType.E_VISA ->
            "Виза нужна, но заявление и разрешение оформляются электронно до поездки."

        VisaType.VISA_REQUIRED ->
            "Для обычной поездки требуется заранее получить визу."

        VisaType.ENTRY_RESTRICTED ->
            "Обычный туристический въезд для выбранного паспорта ограничен. Это не означает, " +
                "что въезд невозможен для всех категорий путешественников."

        VisaType.SPECIAL_PERMIT ->
            "Требуется разрешение специального типа. Проверяйте порядок оформления и доступ " +
                "на официальном сайте территории."

        VisaType.MIXED_REQUIREMENTS ->
            "Разные части территории или разные маршруты могут иметь разные правила. " +
                "Borderly не заменяет их одним вводящим в заблуждение цветом."

        VisaType.NO_DATA ->
            "Borderly не показывает предположение, если для этой пары стран нет достаточно " +
                "надёжного визового статуса."
    }

    val showPassportNote = visaType == VisaType.FREEDOM ||
        visaType == VisaType.ENTRY_RESTRICTED ||
        visaType == VisaType.SPECIAL_PERMIT ||
        visaType == VisaType.MIXED_REQUIREMENTS ||
        !requirement?.note.isNullOrBlank() ||
        visaType == VisaType.NO_DATA

    val showStatusExplanation = visaType == VisaType.FREEDOM ||
        visaType == VisaType.ETA ||
        visaType == VisaType.VISA_ON_ARRIVAL ||
        visaType == VisaType.E_VISA ||
        visaType == VisaType.VISA_REQUIRED ||
        visaType == VisaType.ENTRY_RESTRICTED ||
        visaType == VisaType.SPECIAL_PERMIT ||
        visaType == VisaType.MIXED_REQUIREMENTS ||
        visaType == VisaType.NO_DATA

    val warning = when (visaType) {
        VisaType.ENTRY_RESTRICTED ->
            "Перед покупкой билетов проверьте официальные исключения именно для вашей цели поездки."

        VisaType.SPECIAL_PERMIT ->
            "Не покупайте невозвратные билеты до получения требуемого специального разрешения."

        VisaType.MIXED_REQUIREMENTS ->
            "Проверьте официальный источник для конкретной территории и маршрута поездки."

        VisaType.NO_DATA ->
            "Не планируйте въезд только по этой карточке: сначала проверьте официальный источник."

        else -> null
    }

    return CountryInfo(
        isoNumeric = countryIso,
        flag = flag.ifBlank { "🌍" },
        name = name,
        region = passportRegionFor(countryIso).title,
        visaType = visaType,
        stay = stay,
        stayDays = days,
        entryConditions = entryConditions,
        beforeTrip = beforeTrip,
        applicationDocumentsTitle = applicationDocumentsTitle,
        applicationDocuments = applicationDocuments,
        applicationDocumentsNote = applicationDocumentsNote,
        passportNote = buildString {
            append(passportNote)
            requirement?.note?.takeIf { it.isNotBlank() }?.let { note ->
                append("\n\nПримечание источника: ")
                append(note)
            }
            requirement?.validUntil?.takeIf { it.isNotBlank() }?.let { validUntil ->
                append("\n\nУказанный режим действует до: ")
                append(validUntil)
            }
        },
        showPassportNote = showPassportNote,
        statusExplanation = statusExplanation,
        showStatusExplanation = showStatusExplanation,
        warning = warning,
        entryRequirements = entryRequirements,
        entryGuide = entryGuide,
        source = requirement?.source,
        sourceUrl = requirement?.sourceUrl,
        sourceUpdated = requirement?.updated,
        sourceType = requirement?.sourceType ?: VisaSourceType.UNKNOWN,
        sourceDescription = requirement?.sourceDescription,
        sourceLicense = requirement?.sourceLicense,
        sourceIsRuleSpecific = requirement?.sourceIsRuleSpecific == true
    )
}


