package io.testaxis.backend.actions

import io.testaxis.backend.events.BuildWasCreatedEvent
import io.testaxis.backend.http.transformers.BuildTransformer
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class NotifyUsersOfNewBuild(val simpMessagingTemplate: SimpMessagingTemplate, val transformer: BuildTransformer) {
    @EventListener
    fun handleBuildWasCreated(event: BuildWasCreatedEvent) =
        simpMessagingTemplate.convertAndSend("/topic/builds", transformer.summary(event.build))
}
