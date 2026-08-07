package de.mickkc.vibin.templates.security

import de.mickkc.vibin.templates.TemplateTranslator
import io.ktor.server.html.Template
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.img
import kotlinx.html.p

class DoneTemplate : Template<FlowContent> {

    override fun FlowContent.apply() {
        div("row") {
            img("Checkmark icon", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAADUUlEQVRIS7WVQWwTRxSG/zd2EuJDK6Ge6IVeCE28biJom6YgATekqlIFqipR2A0ha6eXikjtDQlxReXQA4nXEHubnHKw1As3iIREW2iqyN44GCJRCc4IKhoDYXcemnV2WKdO2iLynd787/n9O2/Gu4QthnS0RfyrgVv7cYC5q0xMO7XIgCS+FwTy6Kn+sQWtt2FDg8uViz2dIlmXWokjwRAgXjPrkLuGe3PLOh2jrYHrXZyGTH4NAciAG6JDHjT7xm7pAgDFpcKg8OU1AN2CCAGhZKXtYV2wxj8MXM8pM+MLIoDZ32tlvvlDJ9vgVic/AYlfVCzB5WEjeyTKKVoMZmozvYFs1ASArvTfqS9p/KlObsLsg9nup4//aqh5ie2J3cffHbkT5VoM3OokK4V93mcNjN3Qif/AdKWwXwq+ro7FMmzdVwel24U9tBrMQ4gnpmG/FembMVub7WwEj8pWJvuZWrueswIgxSv+XmuwOVpt4Hr5OkA9YDlkZnK/RvpGnK31db7nf/tcXQSAz5tG9vupysShhEhcZfCSZWT7VCZmMMmAgBnb3kbk5/Md27p4VdUzsGQZdthM4XqOmpLu88qg4qir3WJQ8pyH20ge+iqdq0RaszmthgtCzUzb6bVUSKniMAmCaYyuM/AKrG5BZFBcLJwRzOdUTAlhnOg9tTg3dzZ5/50dL5QWH0Oc5g4kTCO33qB1a6G26PwAxrj6gWTKCKKqKiKgbhr2+1FdHNUnfpN0s1LN+RMSOyXw6UnDDv84itAkwHjzMMMnr1tGtm3z4vLEAfEsMcfEy1Y6u0tp2mDac4YA3AiAJ9a6a1qqOheI5WkmqluZ9s0VRc9ZEUAqkDx48oPsTaVpA0U0Jh/ywxEjNx/pCtcrfGcao+e1sI7izcJ+keLrKo6PucXgp8qlNAvpqbj7f7wqTj+40N3/ONVQ15ZAvSeM0dtRrsVA4Vadn0H4XMXPCR/Zafv3KNeOqUr+44Sg35orLpubvewiLnvOTBJ0TB0pAY98yYejmUZMLThDIokrCeBt9c1g8IxlZI/rgjXaGiiK1fxuQaS32kSVh8fUSoAes9++q9cxNjSIcGsTA+yLKRLUH/YmQDDwQvBCwpcj5ut+Mt8UW27wEmGdWCiU5klqAAAAEGRlQkc0OTY4QkExNjQ1RDNCMjRDsji7XAAAAABJRU5ErkJggg==") {
                width = "24"
                height = "24"
            }
            h2 { +TemplateTranslator.getTranslation("web.security.done.title") }
        }
        p("desc") {
            +TemplateTranslator.getTranslation("web.security.done.desc")
        }
    }
}