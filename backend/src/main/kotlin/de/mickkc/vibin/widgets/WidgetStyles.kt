package de.mickkc.vibin.widgets

import kotlinx.css.*
import kotlinx.css.properties.TextDecoration

object WidgetStyles {

    fun render(): String = CssBuilder().apply {

        root {
            fontFamily = "Arial, sans-serif"
            put("-ms-overflow-style", "none")
            put("scrollbar-width", "none")
        }

        rule("*::-webkit-scrollbar") {
            display = Display.none
        }

        rule(".widget-body") {
            padding = Padding(all = 1.rem)
        }

        p {
            margin = Margin(all = 0.rem)
            padding = Padding(all = 0.rem)
        }

        h1 {
            margin = Margin(all = 0.rem)
            padding = Padding(all = 0.rem)
        }

        h2 {
            margin = Margin(left = 0.rem, top = 0.rem, bottom = 1.rem, right = 0.rem)
            padding = Padding(all = 0.rem)
        }

        rule(".activity-grid") {
            display = Display.grid
            gridTemplateRows = GridTemplateRows("repeat(7, 14px)")
            gap = 4.px
            overflowX = Overflow.scroll
            gridAutoFlow = GridAutoFlow.column
        }

        rule(".activity-grid > div") {
            borderRadius = 2.px
            height = 14.px
            minWidth = 14.px
            display = Display.inlineBlock
        }

        rule(".btn") {
            padding = Padding(vertical = 0.5.rem, horizontal = 1.rem)
            textDecoration = TextDecoration.none
            borderRadius = 4.px
            fontWeight = FontWeight.bold
            width = LinearDimension.fitContent
        }

        rule(".user-container") {
            display = Display.flex
            flexDirection = FlexDirection.row
            alignItems = Align.start
            gap = 2.rem
        }

        media("(max-width: 720px)") {
            rule(".user-container") {
                flexDirection = FlexDirection.column
                alignItems = Align.center
                textAlign = TextAlign.center
            }

            rule(".user-info") {
                alignItems = Align.center
            }
        }

        rule(".user-container img") {
            borderRadius = 50.pct
            width = 192.px
            height = 192.px
            objectFit = ObjectFit.cover
        }

        rule(".user-info") {
            display = Display.flex
            flexDirection = FlexDirection.column
            margin = Margin(all = 1.rem)
            gap = 0.5.rem
            flex = Flex.GROW_SHRINK
            minWidth = 0.px
            minHeight = 0.px
        }

        rule(".user-info .description") {
            margin = Margin(top = 0.5.rem, bottom = 0.5.rem)
            opacity = 0.8
            fontSize = 0.9.rem
            wordWrap = WordWrap.breakWord
        }

        rule(".description:empty") {
            display = Display.none
        }

        rule(".stats") {
            display = Display.flex
            flexDirection = FlexDirection.row
            gap = 1.rem
            marginTop = 1.rem
            alignItems = Align.center
            justifyContent = JustifyContent.spaceAround
            flexWrap = FlexWrap.wrap
        }

        rule(".stat-card") {
            padding = Padding(all = 1.rem)
            display = Display.flex
            alignItems = Align.center
            borderRadius = 0.5.rem
            flexDirection = FlexDirection.column
            flex = Flex.GROW
            textAlign = TextAlign.center
            height = LinearDimension.maxContent
        }

    }.toString()

}