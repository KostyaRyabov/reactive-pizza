package ru.misis.payment.model

import java.time.LocalDateTime

case class Payment(
                 id: String,
                 price: Double,
                 datetime: String = LocalDateTime.now().toString,
               )
