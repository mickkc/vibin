package utils

import org.junit.jupiter.api.assertNotNull
import de.mickkc.vibin.db.tags.TagEntity
import de.mickkc.vibin.dto.tags.TagEditDto
import de.mickkc.vibin.repos.TagRepo

object TagTestUtils {

    fun createTag(
        name: String,
        description: String = "",
        importance: Int = 10
    ): TagEntity {
        val editDto = TagEditDto(
            name = name,
            description = description,
            importance = importance
        )
        val tag = TagRepo.create(editDto)
        assertNotNull(tag)
        return tag
    }

    fun getOrCreateTag(name: String): TagEntity {
        val existingTag = TagRepo.getByName(name)
        if (existingTag != null) {
            return existingTag
        }
        return createTag(name)
    }
}
