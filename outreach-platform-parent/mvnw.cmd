@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Begin all REM://!/sym processing
@setlocal

@set WRAPPER_VERSION=3.3.2

@set __MVNW_CMD__=%~dp0

@IF NOT EXIST "%__MVNW_CMD__%\.mvn\wrapper\maven-wrapper.properties" (
    @echo Cannot find %__MVNW_CMD__%\.mvn\wrapper\maven-wrapper.properties
    @exit /B 1
)

@REM Determine Maven command to use
@set MVNW_REPOURL=
@for /F "usebackq tokens=1,2 delims==" %%A IN ("%__MVNW_CMD__%\.mvn\wrapper\maven-wrapper.properties") DO (
    @if "%%A"=="distributionUrl" @set MVNW_REPOURL=%%B
)

@IF "%MVNW_REPOURL%"=="" (
    @echo distributionUrl not found in %__MVNW_CMD__%\.mvn\wrapper\maven-wrapper.properties
    @exit /B 1
)

@echo Using Maven wrapper
@echo Downloading from: %MVNW_REPOURL%

@REM Fallback to system Maven if wrapper download fails
@where mvn >nul 2>&1
@IF %ERRORLEVEL% EQU 0 (
    mvn %*
) ELSE (
    @echo Maven not found. Please install Maven or ensure the wrapper can download it.
    @exit /B 1
)
