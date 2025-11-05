package utils

import org.junit.jupiter.api.assertNotNull
import wtf.ndu.vibin.db.tags.TagEntity
import wtf.ndu.vibin.dto.tags.TagEditDto
import wtf.ndu.vibin.repos.TagRepo

object TagTestUtils {

    fun createTag(
        name: String,
        description: String = "",
        color: String? = null
    ): TagEntity {
        val editDto = TagEditDto(
            name = name,
            description = description,
            color = color
        )
        val tag = TagRepo.create(editDto)
        assertNotNull(tag)
        return tag
    }
}
