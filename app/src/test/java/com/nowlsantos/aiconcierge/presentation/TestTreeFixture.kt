package com.nowlsantos.aiconcierge.presentation

import com.nowlsantos.common.DocumentNode
import com.nowlsantos.common.UiTemplate

object TestTreeFixture {
    val chineseTea = DocumentNode(
        name = "Chinese Tea",
        content = "Premium Oolong or Jasmine tea.",
        level = 6,
        price = 0.00
    )

    val dumplings = DocumentNode(
        name = "Har Gow, Siu Mai and Fun Guo",
        content = "Classic trio of dim sum.",
        level = 6,
        keywords = "har gow, siu mai, dumplings, dim sum",
        price = 42.00
    )

    val congee = DocumentNode(
        name = "Congee with condiments",
        content = "Silky rice porridge.",
        level = 6,
        keywords = "congee, porridge",
        price = 0.00
    )

    val cantoneseBreakfast = DocumentNode(
        name = "Cantonese Breakfast",
        content = "A taste from East at the western hemisphere.",
        level = 5,
        uiTemplate = UiTemplate.SELECTION_VIEW,
        displayType = "ACTION",
        imageAsset = "cantonese_breakfast_hero",
        price = 48.00,
        children = listOf(
            DocumentNode(name = "Hot Beverage", content = "Hot drinks", level = 6, children = listOf(chineseTea)),
            DocumentNode(name = "Selection of Dumplings", content = "Dim sum", level = 6, children = listOf(dumplings)),
            DocumentNode(name = "Congee", content = "Porridge", level = 6, children = listOf(congee))
        )
    )

    val breakfastSets = DocumentNode(
        name = "Breakfast Sets",
        content = "Breakfast selections available until 11:00 AM.",
        level = 4,
        uiTemplate = UiTemplate.CHOICE_LIST,
        displayType = "LIST",
        children = listOf(cantoneseBreakfast)
    )

    val inRoomDining = DocumentNode(
        name = "In-Room Dining Ordering",
        content = "Experience exceptional cuisine.",
        level = 3,
        uiTemplate = UiTemplate.CHOICE_LIST,
        iconName = "dining",
        keywords = "in room dining, room service, order food, room dining, in-room dining, in room dining ordering, delivery",
        displayType = "LIST",
        children = listOf(breakfastSets)
    )

    val diningCategory = DocumentNode(
        name = "Dining",
        content = "Gourmet selections.",
        level = 2,
        uiTemplate = UiTemplate.CATEGORY_GRID,
        iconName = "dining",
        displayType = "GRID",
        children = listOf(inRoomDining)
    )

    val bbcNews = DocumentNode(
        name = "BBC World News",
        content = "24-hour international news coverage.",
        level = 4,
        uiTemplate = UiTemplate.DETAIL_VIEW,
        iconName = "media",
        displayType = "ACTION",
        keywords = "bbc, bbc news, bbc world news, news, live news, tv news, news on tv, television news"
    )

    val cnnNews = DocumentNode(
        name = "CNN International",
        content = "Global news and breaking updates from CNN.",
        level = 4,
        uiTemplate = UiTemplate.DETAIL_VIEW,
        iconName = "media",
        displayType = "ACTION",
        keywords = "cnn, cnn international, news, global news, breaking news"
    )

    val tvCategory = DocumentNode(
        name = "TV",
        content = "Global broadcasting channels.",
        level = 3,
        uiTemplate = UiTemplate.CHOICE_LIST,
        iconName = "media",
        displayType = "GRID",
        children = listOf(bbcNews, cnnNews)
    )

    val mediaCategory = DocumentNode(
        name = "Media",
        content = "Curated media library.",
        level = 2,
        uiTemplate = UiTemplate.CATEGORY_GRID,
        iconName = "media",
        displayType = "GRID",
        children = listOf(tvCategory)
    )

    val bletchleyPark = DocumentNode(
        name = "Bletchley Park Tour",
        content = "Private tour of Bletchley Park with Sir Dermot Turing.",
        level = 4,
        uiTemplate = UiTemplate.DETAIL_VIEW,
        iconName = "services",
        displayType = "ACTION",
        keywords = "bletchley park, code breakers, tour"
    )

    val peninsulaAcademy = DocumentNode(
        name = "Peninsula Academy",
        content = "Bespoke cultural masterclasses and experiences.",
        level = 4,
        uiTemplate = UiTemplate.CHOICE_LIST,
        iconName = "services",
        displayType = "LIST",
        keywords = "peninsula academy, pen academy, academy",
        children = listOf(
            DocumentNode(name = "About The Academy", content = "Details about academy", level = 5, uiTemplate = UiTemplate.DETAIL_VIEW)
        )
    )

    val guestServicesCategory = DocumentNode(
        name = "Guest Services",
        content = "Guest services directory.",
        level = 2,
        uiTemplate = UiTemplate.CATEGORY_GRID,
        iconName = "services",
        displayType = "GRID",
        children = listOf(bletchleyPark, peninsulaAcademy)
    )

    val spaCategory = DocumentNode(
        name = "Spa and Wellness",
        content = "Serene oasis.",
        level = 2,
        uiTemplate = UiTemplate.CATEGORY_GRID,
        iconName = "spa",
        displayType = "GRID",
        children = emptyList()
    )

    val rootHome = DocumentNode(
        name = "Home",
        content = "Welcome back to Peninsula Hotel.",
        level = 1,
        uiTemplate = UiTemplate.RIBBON_HUB,
        displayType = "GRID",
        children = listOf(diningCategory, mediaCategory, guestServicesCategory, spaCategory)
    )
}
