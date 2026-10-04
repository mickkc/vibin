package de.mickkc.vibin.widgets

import kotlinx.css.*
import kotlinx.css.properties.Animation
import kotlinx.css.properties.IterationCount
import kotlinx.css.properties.TextDecoration
import kotlinx.css.properties.Timing
import kotlinx.css.properties.Transform
import kotlinx.css.properties.Transition
import kotlinx.css.properties.s

object WidgetStyles {

    fun render(): String = CssBuilder().apply {

        rule(".vibin-widget-container") {
            fontFamily = "Arial, sans-serif"
            containerName = ContainerName("vibinWidgetContainer")
            containerType = ContainerType.inlineSize
            put("-ms-overflow-style", "none")
            put("scrollbar-width", "none")
            height = LinearDimension.fitContent
        }

        rule(".vibin-widget-container *::-webkit-scrollbar") {
            display = Display.none
        }

        rule(".vibin-widget-container .widget-body") {
            padding = Padding(all = 1.rem)
        }

        rule(".vibin-widget-container p") {
            margin = Margin(all = 0.rem)
            padding = Padding(all = 0.rem)
        }

        rule(".vibin-widget-container h1") {
            margin = Margin(all = 0.rem)
            padding = Padding(all = 0.rem)
        }

        rule(".vibin-widget-container h2") {
            margin = Margin(left = 0.rem, top = 0.rem, bottom = 1.rem, right = 0.rem)
            padding = Padding(all = 0.rem)
        }

        rule(".vibin-widget-container h3") {
            margin = Margin(all = 0.rem)
            padding = Padding(all = 0.rem)
        }

        rule(".vibin-widget-container .activity-grid") {
            display = Display.grid
            gridTemplateRows = GridTemplateRows("repeat(7, 14px)")
            gap = 4.px
            overflowX = Overflow.scroll
            gridAutoFlow = GridAutoFlow.column
        }

        rule(".vibin-widget-container .activity-grid > div") {
            borderRadius = 2.px
            height = 14.px
            minWidth = 14.px
            display = Display.inlineBlock
        }

        rule(".vibin-widget-container .btn") {
            padding = Padding(vertical = 0.5.rem, horizontal = 1.rem)
            textDecoration = TextDecoration.none
            borderRadius = 4.px
            fontWeight = FontWeight.bold
            width = LinearDimension.fitContent
        }

        rule(".vibin-widget-container .user-container") {
            display = Display.flex
            flexDirection = FlexDirection.row
            alignItems = Align.start
            gap = 2.rem
        }

        container("vibinWidgetContainer (max-width: 720px)") {
            rule(".vibin-widget-container .user-container") {
                flexDirection = FlexDirection.column
                alignItems = Align.center
                textAlign = TextAlign.center
            }

            rule(".vibin-widget-container .user-info") {
                alignItems = Align.center
            }
        }

        rule(".vibin-widget-container .user-container img") {
            borderRadius = 50.pct
            width = 192.px
            height = 192.px
            objectFit = ObjectFit.cover
        }

        rule(".vibin-widget-container .user-info") {
            display = Display.flex
            flexDirection = FlexDirection.column
            margin = Margin(all = 1.rem)
            gap = 0.5.rem
            flex = Flex.GROW_SHRINK
            minWidth = 0.px
            minHeight = 0.px
        }

        rule(".vibin-widget-container .description") {
            margin = Margin(top = 0.5.rem, bottom = 0.5.rem)
            opacity = 0.8
            fontSize = 0.9.rem
            wordWrap = WordWrap.breakWord
        }

        rule(".vibin-widget-container .description:empty") {
            display = Display.none
        }

        rule(".vibin-widget-container .stats") {
            display = Display.flex
            flexDirection = FlexDirection.row
            gap = 1.rem
            marginTop = 1.rem
            alignItems = Align.center
            justifyContent = JustifyContent.spaceAround
            flexWrap = FlexWrap.wrap
        }

        rule(".vibin-widget-container .stat-card") {
            padding = Padding(all = 1.rem)
            display = Display.flex
            alignItems = Align.center
            borderRadius = 0.5.rem
            flexDirection = FlexDirection.column
            flex = Flex.GROW
            textAlign = TextAlign.center
            height = LinearDimension.maxContent
        }

        rule(".vibin-widget-container .favorites") {
            display = Display.flex
            flexDirection = FlexDirection.column
        }

        rule(".vibin-widget-container .items") {
            display = Display.flex
            flexDirection = FlexDirection.row
            gap = 1.rem
            overflowX = Overflow.scroll
        }

        rule(".vibin-widget-container .item") {
            display = Display.flex
            flexDirection = FlexDirection.column
            gap = 0.25.rem
            padding = Padding(all = 0.5.rem)
            borderRadius = 0.5.rem
            position = Position.relative
        }

        rule(".vibin-widget-container .item.item-artist img") {
            borderRadius = 50.pct
        }

        rule(".vibin-widget-container .item-place") {
            position = Position.absolute
            top = 0.5.rem
            right = 1.rem
            fontSize = 1.5.rem
            padding = Padding(horizontal = 0.25.rem, vertical = 0.5.rem)
            borderBottomLeftRadius = 0.5.rem
            borderBottomRightRadius = 0.5.rem
            userSelect = UserSelect.none
        }

        rule(".vibin-widget-container .item-info") {
            display = Display.flex
            flexDirection = FlexDirection.column
            gap = 0.25.rem
        }

        rule(".vibin-widget-container .item-info > *") {
            overflow = Overflow.hidden
            textOverflow = TextOverflow.ellipsis
            whiteSpace = WhiteSpace.nowrap
            maxWidth = 128.px
        }

        rule(".vibin-widget-container .item-subtitle") {
            padding = Padding(all = 0.rem)
            margin = Margin(all = 0.rem)
            opacity = 0.8
            fontSize = 0.9.rem
        }

        rule(".vibin-widget-container .item-cover") {
            width = 128.px
            height = 128.px
            objectFit = ObjectFit.cover
            borderRadius = 0.5.rem
        }

        rule(".vibin-widget-container .joined-favorites") {
            display = Display.flex
            flexDirection = FlexDirection.row
            gap = 2.rem
            flexWrap = FlexWrap.wrap
            justifyContent = JustifyContent.start
            alignItems = Align.center
        }

        container("vibinWidgetContainer (max-width: 1000px)") {
            rule(".vibin-widget-container .joined-favorites") {
                flexDirection = FlexDirection.column
                alignItems = Align.center
                justifyContent = JustifyContent.center
                width = LinearDimension("100cqi") - 2.rem
                gap = 1.rem
            }
            rule(".vibin-widget-container .joined-favorites .items") {
                width = LinearDimension("100cqi") - 4.rem
            }
        }

        rule(".vibin-widget-container .listening-to") {
            display = Display.flex
            flexDirection = FlexDirection.row
            gap = 1.rem
            alignItems = Align.start

            rule(".details") {
                fontSize = 1.5.rem
                fontWeight = FontWeight.w700
                paddingTop = 0.5.rem
                flex = Flex.GROW_SHRINK
            }

            rule(".artists") {
                opacity = 0.8
                fontSize = 1.2.rem
            }

            rule(".state") {
                opacity = 0.5
                fontSize = 0.9.rem
                marginTop = 0.75.rem
                marginBottom = 0.75.rem
            }
        }

        rule(".vibin-widget-container .progress") {
            width = 100.pct
            display = Display.flex
            flexDirection = FlexDirection.row
            gap = 1.rem
            alignItems = Align.center
        }

        rule(".vibin-widget-container .progress > p") {
            fontSize = 1.rem
        }

        rule(".vibin-widget-container .progress-container") {
            height = 5.px
            borderRadius = 2.5.px
            overflow = Overflow.hidden
            flex = Flex.GROW
        }

        rule(".vibin-widget-container .progress-bar") {
            height = 100.pct
            width = 0.pct
            borderRadius = 2.5.px
            transition += Transition("width", 1.s, Timing.ease)
        }

        keyframes("vibinProgressBarIndeterminate") {
            from { transform += Transform("translateX", arrayOf((-100).pct)) }
            to { transform += Transform("translateX", arrayOf(350.pct)) }
        }

        rule(".vibin-widget-container .progress-bar.indeterminate") {
            width = 40.pct
            animation += Animation(
                name = "vibinProgressBarIndeterminate",
                duration = 1.5.s,
                timing = Timing.easeInOut,
                iterationCount = IterationCount.infinite
            )
        }

    }.toString()

}