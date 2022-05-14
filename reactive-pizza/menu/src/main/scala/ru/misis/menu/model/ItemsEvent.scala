package ru.misis.menu.model

import ru.misis.event.Event

case class ItemsEvent(items: Seq[Item]) extends Event
