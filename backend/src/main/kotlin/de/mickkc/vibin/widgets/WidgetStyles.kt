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

        h3 {
            margin = Margin(all = 0.rem)
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

        rule(".description") {
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

        rule(".favorites") {
            display = Display.flex
            flexDirection = FlexDirection.column
        }

        rule(".favorite-items") {
            display = Display.flex
            flexDirection = FlexDirection.row
            gap = 1.rem
            overflowX = Overflow.scroll
        }

        rule(".favorite-item") {
            display = Display.flex
            flexDirection = FlexDirection.column
            gap = 0.25.rem
            padding = Padding(all = 0.5.rem)
            borderRadius = 0.5.rem
            position = Position.relative
        }

        rule(".favorite-place") {
            position = Position.absolute
            top = 0.5.rem
            right = 1.rem
            fontSize = 1.5.rem
            padding = Padding(horizontal = 0.25.rem, vertical = 0.5.rem)
            borderBottomLeftRadius = 0.5.rem
            borderBottomRightRadius = 0.5.rem
            userSelect = UserSelect.none
        }

        rule(".item-info") {
            display = Display.flex
            flexDirection = FlexDirection.column
            gap = 0.25.rem
        }

        rule(".item-info > *") {
            overflow = Overflow.hidden
            textOverflow = TextOverflow.ellipsis
            whiteSpace = WhiteSpace.nowrap
            maxWidth = 128.px
        }

        rule(".item-subtitle") {
            padding = Padding(all = 0.rem)
            margin = Margin(all = 0.rem)
            opacity = 0.8
            fontSize = 0.9.rem
        }

        rule(".item-cover") {
            width = 128.px
            height = 128.px
            objectFit = ObjectFit.cover
            borderRadius = 0.5.rem
        }

        rule(".joined-favorites") {
            display = Display.flex
            flexDirection = FlexDirection.row
            gap = 2.rem
            flexWrap = FlexWrap.wrap
            justifyContent = JustifyContent.start
            alignItems = Align.center
        }

        media("(max-width: 1000px)") {
            rule(".joined-favorites") {
                flexDirection = FlexDirection.column
                alignItems = Align.center
                justifyContent = JustifyContent.center
                width = 100.vw - 2.rem
                gap = 1.rem
            }
            rule(".joined-favorites .favorite-items") {
                width = 100.vw - 4.rem
            }
        }

    }.toString()

}