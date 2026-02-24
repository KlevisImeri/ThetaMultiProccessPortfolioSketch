# ThetaMultiProccessPortfolioSketch
Just a sketch showing how to desing theta with algorithsm run in multiprocesses.

### Compilation
```
kotlinc -classpath kotlinx-coroutines-core.jar -d out portfolio.kt main.kt logger.kt
```
### Running
Good path (you don't need to spawn process)
```
kotlin -classpath "out:kotlinx-coroutines-core.jar" main.MainKt input.xcfa
```
Faild path (you need tp spawn a process)
```
FAIL_CEGAR=true kotlin -classpath "out:kotlinx-coroutines-core.jar" main.MainKt input.xcfa

```

### Full script
```
rm -f out || true && kotlinc -classpath kotlinx-coroutines-core.jar -d out portfolio.kt main.kt logger.kt && kotlin -classpath "out:kotlinx-coroutines-core.jar" main.MainKt input.xcfa && FAIL_CEGAR=true kotlin -classpath "out:kotlinx-coroutines-core.jar" main.MainKt input.xcfa

```


