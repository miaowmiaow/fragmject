package com.example.fragment.project.appwidgets

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MyAppWidgetReceiver : GlanceAppWidgetReceiver() {

    // Let MyAppWidgetReceiver know which GlanceAppWidget to use
    override val glanceAppWidget: GlanceAppWidget = MyAppWidget()
}