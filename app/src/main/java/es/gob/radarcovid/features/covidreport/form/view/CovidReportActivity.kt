/*
 * Copyright (c) 2020 Gobierno de España
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package es.gob.radarcovid.features.covidreport.form.view

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import es.gob.radarcovid.R
import es.gob.radarcovid.common.base.BaseBackNavigationActivity
import es.gob.radarcovid.common.view.CMDialog
import es.gob.radarcovid.features.covidreport.form.protocols.CovidReportPresenter
import es.gob.radarcovid.features.covidreport.form.protocols.CovidReportView
import kotlinx.android.synthetic.main.activity_covid_report.*
import kotlinx.android.synthetic.main.layout_back_navigation.*
import kotlinx.android.synthetic.main.layout_select_date_symptom.view.*
import org.dpppt.android.sdk.DP3T
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class CovidReportActivity : BaseBackNavigationActivity(), CovidReportView {

    companion object {

        fun open(context: Context) {
            context.startActivity(Intent(context, CovidReportActivity::class.java))
        }

    }

    @Inject
    lateinit var presenter: CovidReportPresenter

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        DP3T.onActivityResult(this, requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_covid_report)

        initViews()
        presenter.viewReady()
    }

    override fun onResume() {
        super.onResume()
        if (isAccessibilityEnabled()) {
            codeEditText.visibility = View.INVISIBLE
            editTextCodeAccessibility.visibility = View.VISIBLE
            presenter.onCodeChanged(editTextCodeAccessibility.text.toString())
        } else {
            codeEditText.visibility = View.VISIBLE
            editTextCodeAccessibility.visibility = View.GONE
            presenter.onCodeChanged(codeEditText.getText())
        }
    }

    override fun onPause() {
        super.onPause()
        hideKeyBoard()
    }

    private fun initViews() {
        imageButtonBack.contentDescription =
            "${labelManager.getText("ACC_HOME_TITLE", R.string.title_home)} ${
                labelManager.getText(
                    "ACC_BUTTON_BACK_TO",
                    R.string.navigation_back_to
                )
            }"

        buttonSend.setOnClickListener {
            hideKeyBoard()
            presenter.onSendButtonClick()
        }
        codeEditText.textChangedListener = {
            presenter.onCodeChanged(it)
        }
        editTextCodeAccessibility.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                presenter.onCodeChanged(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        select_date.setOnClickListener { presenter.onSelectDateClick() }
    }

    override fun onBackPressed() {
        presenter.onBackPressed()
    }

    override fun onBackArrowClick(view: View) {
        hideKeyBoard()
        presenter.onBackPressed()
    }

    override fun showExitConfirmationDialog() {
        CMDialog.Builder(this)
            .setTitle(
                labelManager.getText(
                    "ALERT_MY_HEALTH_SEND_TITLE",
                    R.string.covid_report_abort_warning_title
                ).toString()
            )
            .setMessage(
                labelManager.getText(
                    "ALERT_MY_HEALTH_SEND_CONTENT",
                    R.string.covid_report_abort_warning_message
                ).toString()
            )
            .setPositiveButton(
                labelManager.getText(
                    "ALERT_CANCEL_SEND_BUTTON",
                    R.string.covid_report_abort_send_warning_button
                ).toString()
            ) {
                it.dismiss()
                presenter.onExitConfirmed()
            }
            .setNegativeButton(
                labelManager.getText(
                    "ACC_BUTTON_CLOSE",
                    R.string.dialog_close_button_description
                ).toString()
            ) {
                it.dismiss()
            }
            .build()
            .show()
    }

    override fun getReportCode(): String =
        if (isAccessibilityEnabled())
            editTextCodeAccessibility.text.toString()
        else
            codeEditText.getText()

    override fun setButtonSendEnabled(enabled: Boolean) {
        buttonSend.isEnabled = enabled
    }

    override fun hideLoadingWithNetworkError() {
        hideLoadingWithError(
            labelManager.getText(
                "ALERT_NETWORK_ERROR_TITLE",
                R.string.network_warning_dialog_title
            ).toString(),
            labelManager.getText(
                "ALERT_POSITIVE_REPORT_NETWORK_ERROR_MESSAGE",
                R.string.network_warning_dialog_message
            ).toString(),
            labelManager.getText(
                "ALERT_RETRY_BUTTON",
                R.string.network_warning_dialog_button
            ).toString()
        ) {
            presenter.onRetryButtonClick()
        }
    }

    override fun hideLoadingWithErrorOnReport() {
        hideLoadingWithError(
            Exception(
                labelManager.getText(
                    "ALERT_MY_HEALTH_CODE_ERROR_CONTENT",
                    R.string.covid_report_error
                ).toString()
            )
        )
    }

    override fun showDatePickerDialog() {

        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->

                select_date.layout_day.button_day.text =
                    if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                select_date.layout_month.button_month.text =
                    if (monthOfYear < 10) "0$monthOfYear" else "$monthOfYear"
                select_date.layout_year.button_year.text = year.toString()

            }

        val dpd = DatePickerDialog(this, dateSetListener, year, month, day)

        val newCalendar = Calendar.getInstance()
        dpd.datePicker.maxDate = newCalendar.timeInMillis
        newCalendar.add(Calendar.DATE, - 14)
        dpd.datePicker.minDate = newCalendar.timeInMillis

        dpd.show()
    }

    override fun getDateSelected(): Date? {
        val day = select_date.layout_day.button_day.text
        val month = select_date.layout_day.button_month.text
        val year = select_date.layout_day.button_year.text

        return if (day.isNullOrEmpty() && month.isNullOrEmpty() && year.isNullOrEmpty()) {
            null
        } else {
            return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse("$day-$month-$year")
        }
    }

}