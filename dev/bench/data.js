window.BENCHMARK_DATA = {
  "lastUpdate": 1587564630130,
  "repoUrl": "https://github.com/EightM/bsl-language-server",
  "entries": {
    "BSL LS perfomance measurement (SSL 3.1)": [
      {
        "commit": {
          "author": {
            "email": "nixel2007@gmail.com",
            "name": "Nikita Gryzlov",
            "username": "nixel2007"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "538e995dda77051e94b3afcff83911ac7b27e6e7",
          "message": "Merge pull request #1103 from 1c-syntax/fix/docFix\n\ndoc fix",
          "timestamp": "2020-04-22T10:54:05+03:00",
          "tree_id": "8bbaba019dde5f548238ad745d449d821ffe578f",
          "url": "https://github.com/EightM/bsl-language-server/commit/538e995dda77051e94b3afcff83911ac7b27e6e7"
        },
        "date": 1587554332273,
        "tool": "pytest",
        "benches": [
          {
            "name": ".github/scripts/benchmark.py::test_analyze_ssl31",
            "value": 2.6225639979044595,
            "unit": "sec",
            "range": "stddev: 0.26622370145771235",
            "extra": "mean: 2.6225639979044595 sec\nrounds: 3"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nixel2007@gmail.com",
            "name": "Nikita Gryzlov",
            "username": "nixel2007"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "76fcf12f7d4983a508893bf2848238b70692dc44",
          "message": "Merge pull request #1100 from yukon39/feature/CoverageAnalysis\n\nДобавлена возможность по выводу в отчет строк требующих покрытия",
          "timestamp": "2020-04-22T14:52:11+03:00",
          "tree_id": "563281f2a9b78d2c8704bd0ff4c18be558531d51",
          "url": "https://github.com/EightM/bsl-language-server/commit/76fcf12f7d4983a508893bf2848238b70692dc44"
        },
        "date": 1587564629327,
        "tool": "pytest",
        "benches": [
          {
            "name": ".github/scripts/benchmark.py::test_analyze_ssl31",
            "value": 71.14618460337321,
            "unit": "sec",
            "range": "stddev: 1.0168344031623033",
            "extra": "mean: 71.14618460337321 sec\nrounds: 3"
          }
        ]
      }
    ]
  }
}