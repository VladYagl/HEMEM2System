const val ISSUES = "https://api.github.com/repos/VladYagl/HEMEM2System/issues"

data class Issue (
        val id: Int,
        val html_url: String,
        val title: String,
        val state: String,
        val body: String?,
)
