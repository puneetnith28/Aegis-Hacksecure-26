package com.obsidian.aegis.ui.risk

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.obsidian.aegis.databinding.ActivityRiskBinding
import com.obsidian.aegis.ui.adapters.RiskAppsAdapter

class RiskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiskBinding
    private lateinit var viewModel: RiskViewModel
    private lateinit var riskAppsAdapter: RiskAppsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(RiskViewModel::class.java)
        riskAppsAdapter = RiskAppsAdapter()

        setupRecyclerView()
        setupObservers()

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val usageStatsHelper = com.obsidian.aegis.helpers.UsageStatsHelper(this)
        if (!usageStatsHelper.hasUsageStatsPermission()) {
            usageStatsHelper.requestUsageStatsPermission()
        }

        viewModel.loadRiskData()
    }

    override fun onResume() {
        super.onResume()
        val usageStatsHelper = com.obsidian.aegis.helpers.UsageStatsHelper(this)
        if (usageStatsHelper.hasUsageStatsPermission()) {
            viewModel.loadRiskData()
        }
    }

    private fun setupRecyclerView() {
        binding.rvRiskApps.apply {
            adapter = riskAppsAdapter
            layoutManager = LinearLayoutManager(this@RiskActivity)
        }

        riskAppsAdapter.setOnItemClickListener { appRisk ->
            // Phase 2: Open Specific Details Page and Chart
            val bottomSheet = RiskBottomSheetFragment.newInstance(appRisk.appId, appRisk.appName)
            bottomSheet.show(supportFragmentManager, "RiskBottomSheet")
        }
    }

    private fun setupObservers() {
        viewModel.appRiskScores.observe(this, { scores ->
            riskAppsAdapter.differ.submitList(scores)
            binding.rvRiskApps.visibility = View.VISIBLE
        })

        viewModel.isLoading.observe(this, { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })
    }
}
