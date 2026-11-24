package com.blockreels.app.intervention

import com.blockreels.app.state.InterventionConfig
import com.blockreels.app.state.RuleConfig

object InterventionConfigResolver {
    fun resolve(base: InterventionConfig, rule: RuleConfig?): InterventionConfig {
        return if (rule == null) base else base.copy(mode = rule.mode)
    }
}
