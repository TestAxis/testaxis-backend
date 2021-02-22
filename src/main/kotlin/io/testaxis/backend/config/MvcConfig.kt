package io.testaxis.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class MvcConfig : WebMvcConfigurer {
    override fun addViewControllers(registry: ViewControllerRegistry) {
        registry.addViewController("/experiment").setViewName("redirect:/experiment/")
        registry.addViewController("/experiment/").setViewName("forward:/experiment/index.html")
    }
}
