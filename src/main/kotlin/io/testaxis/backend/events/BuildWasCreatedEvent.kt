package io.testaxis.backend.events

import io.testaxis.backend.models.Build
import org.springframework.context.ApplicationEvent

class BuildWasCreatedEvent(source: Any, val build: Build) : ApplicationEvent(source)
