param([string]$PipeName)
$ErrorActionPreference = 'Stop'
$Mode = if ($args.Count -gt 0) { $args[0] } else { 'activity' }
function Open-Pipe {
    $script:pipe = [System.IO.Pipes.NamedPipeServerStream]::new($PipeName, [System.IO.Pipes.PipeDirection]::InOut, 1,
        [System.IO.Pipes.PipeTransmissionMode]::Byte, [System.IO.Pipes.PipeOptions]::Asynchronous, 65536, 65536)
    $script:reader = [System.IO.BinaryReader]::new($pipe)
    $script:writer = [System.IO.BinaryWriter]::new($pipe)
}
function Read-Frame {
    $op = $reader.ReadInt32()
    $length = $reader.ReadInt32()
    if ($length -lt 0 -or $length -gt 1048576) { throw 'Invalid frame size' }
    $bytes = $reader.ReadBytes($length)
    if ($bytes.Length -ne $length) { throw 'Truncated frame' }
    return @{ Opcode = $op; Text = [Text.Encoding]::UTF8.GetString($bytes) }
}
function Send-Frame([int]$op, [string]$json) {
    $bytes = [Text.Encoding]::UTF8.GetBytes($json)
    $writer.Write($op)
    $writer.Write([int]$bytes.Length)
    $writer.Write($bytes)
    $writer.Flush()
}
function Handshake([string]$id) {
    $pipe.WaitForConnection()
    $frame = Read-Frame
    $json = $frame.Text | ConvertFrom-Json
    if ($frame.Opcode -ne 0 -or $json.v -ne 1 -or $json.client_id -ne $id) { throw 'Bad handshake' }
    # Deliberately split both header and UTF-8 payload across writes.
    $body = [Text.Encoding]::UTF8.GetBytes('{"evt":"READY","data":{"text":"Привет"}}')
    $frame = [BitConverter]::GetBytes([int]1) + [BitConverter]::GetBytes([int]$body.Length) + $body
    $writer.Write($frame, 0, 3)
    $writer.Flush()
    Start-Sleep -Milliseconds 150
    $writer.Write($frame, 3, 10)
    $writer.Flush()
    Start-Sleep -Milliseconds 150
    $writer.Write($frame, 13, $frame.Length - 13)
    $writer.Flush()
}
function Activity([string]$details) {
    $frame = Read-Frame
    $json = $frame.Text | ConvertFrom-Json
    if ($frame.Opcode -ne 1 -or $json.cmd -ne 'SET_ACTIVITY' -or $json.args.pid -le 0 -or !$json.nonce) { throw 'Bad command' }
    if ($json.args.activity.details -ne $details) { throw "Wrong details: $($frame.Text)" }
    return $json.args.activity
}
function Wait-Closed {
    # Pending activity updates may precede EOF.
    try { while ($true) { $null = Read-Frame } }
    catch [System.IO.EndOfStreamException] { }
    catch [System.IO.IOException] { }
    $pipe.Dispose()
}
try {
    Open-Pipe
    Write-Output 'LISTENING'
    if ($Mode -ne 'activity') {
        $pipe.WaitForConnection()
        $probe = Read-Frame
        $handshake = $probe.Text | ConvertFrom-Json
        if ($probe.Opcode -ne 0 -or $handshake.client_id -ne '123456789012345678') { throw 'Bad probe handshake' }
        Write-Output 'PROBE_HANDSHAKE'
        switch ($Mode) {
            'ready' { Send-Frame 1 '{"evt":"READY"}' }
            'reject' { Send-Frame 2 '{"code":4000,"message":"Invalid client_id"}' }
            'error' { Send-Frame 1 '{"evt":"ERROR","data":{"code":4000,"message":"Invalid client_id"}}' }
            'timeout' { }
            'cancel' { }
            default { throw 'Unknown probe mode' }
        }
        try {
            $null = Read-Frame
            throw 'Probe must not publish an activity'
        } catch [System.IO.EndOfStreamException] { }
        catch [System.IO.IOException] { }
        Write-Output 'PROBE_CLOSED'
        return
    }
    Handshake '123456789012345678'
    Write-Output 'HANDSHAKE_OK'
    $activity = Activity 'Test world'
    Write-Output 'ACTIVITY_OK'
    if ($activity.state -ne 'Unicode: Привет 😀' -or $activity.timestamps.start -ne 1234567890) { throw 'Lost world data' }
    Send-Frame 3 '{"ping":"test"}'
    do { $pong = Read-Frame } while ($pong.Opcode -eq 1)
    if ($pong.Opcode -ne 4 -or $pong.Text -ne '{"ping":"test"}') { throw 'Bad pong' }
    Write-Output 'WORLD_OK'
    foreach ($index in 1..3) {
        $null = Activity "Update $index"
        Write-Output "UPDATE_$index"
    }
    $menu = Activity 'В главном меню'
    if ($null -ne $menu.timestamps -or $menu.state -ne 'Ник: Alex') { throw 'Stale menu data' }
    $pipe.Dispose()
    Open-Pipe
    Write-Output 'LISTENING_AGAIN'
    Handshake '123456789012345678'
    $null = Activity 'В главном меню'
    Write-Output 'RECONNECTED'
    Wait-Closed
    Open-Pipe
    Write-Output 'LISTENING_NEW_ID'
    Handshake '234567890123456789'
    $null = Activity 'В главном меню'
    Write-Output 'NEW_ID_OK'
    Wait-Closed
    Write-Output 'CLOSED'
} finally {
    if ($null -ne $pipe) { $pipe.Dispose() }
}
