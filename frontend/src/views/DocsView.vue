<script>
import SidebarBase from '@/components/SidebarBase.vue'
import { ref, onMounted, onBeforeUnmount } from 'vue'

export default {
  name: 'DocsView',
  components: { SidebarBase },
  setup() {
    const ws = ref(null)
    const connecting = ref(false)
    const wsState = ref('closed')
    const outbox = []

    const text = ref('')
    const mirror = ref('')
    const version = ref(0)
    const pending = ref([])
    const clientId = crypto.randomUUID()
    const textarea = ref(null)
    let lastValue = ''

    let reconnectDelay = 500
    let reconnectTimer = null

    const clamp = (x, a, b) => Math.max(a, Math.min(b, x))

    function ensureSingleConnect() {
      if (connecting.value) return false
      const state = ws.value?.readyState
      if (state === WebSocket.OPEN || state === WebSocket.CONNECTING) return false
      return true
    }

    function connect() {
      if (!ensureSingleConnect()) {
        console.log('[OT] connect() ignored; socket already', ws.value?.readyState)
        return
      }
      connecting.value = true
      wsState.value = 'connecting'
      console.log('[OT] Connecting to WebSocket…')

      ws.value = new WebSocket('ws://localhost:8082/ws/doc')

      ws.value.onopen = () => {
        console.log('[OT] ✅ Connected')
        connecting.value = false
        wsState.value = 'open'
        reconnectDelay = 500
        send({ action: 'join', docId: 'demo' })
        drainOutbox()
      }

      ws.value.onclose = (e) => {
        console.warn('[OT] ⚠️ Closed', e.code, e.reason)
        wsState.value = 'closed'
        connecting.value = false
        scheduleReconnect()
      }

      ws.value.onerror = (err) => {
        console.error('[OT] ❌ Error', err)
        try {
          ws.value?.close()
        } catch (e) {
          console.warn('[OT] Socket close threw', e)
        }
      }

      ws.value.onmessage = (ev) => {
        const msg = JSON.parse(ev.data)
        console.log('[OT] 📩 Incoming:', msg)
        if (msg.type === 'snapshot') {
          mirror.value = msg.content || ''
          text.value = mirror.value
          lastValue = mirror.value
          version.value = msg.version
          if (textarea.value) textarea.value.value = mirror.value
          console.log(`[OT] 🧾 Snapshot v${version.value}`)
          return
        }
        integrateServerOp(msg)
      }
    }

    function scheduleReconnect() {
      if (reconnectTimer) return
      reconnectTimer = setTimeout(() => {
        reconnectTimer = null
        console.log('[OT] 🔁 Reconnecting…')
        reconnectDelay = Math.min(reconnectDelay * 2, 8000)
        connect()
      }, reconnectDelay)
    }

    function safeSend(obj) {
      const state = ws.value?.readyState
      if (state === WebSocket.OPEN) {
        ws.value.send(JSON.stringify(obj))
      } else {
        console.warn('[OT] ⏸️ Socket not OPEN (state:', state, '); queuing message')
        outbox.push(obj)
        if (state !== WebSocket.CONNECTING) scheduleReconnect()
      }
    }

    function drainOutbox() {
      if (ws.value?.readyState !== WebSocket.OPEN) return
      while (outbox.length) {
        const m = outbox.shift()
        console.log('[OT] 📨 Draining queued ->', m)
        ws.value.send(JSON.stringify(m))
      }
    }

    function send(obj) {
      console.log('[OT] 📨 Sending:', obj)
      safeSend(obj)
    }

    function integrateServerOp(msg) {
      console.log('[OT] 🔄 Apply from server:', msg)
      if (msg.type === 'insert') {
        const p = clamp(msg.pos, 0, mirror.value.length)
        mirror.value = mirror.value.slice(0, p) + msg.text + mirror.value.slice(p)
      } else if (msg.type === 'delete') {
        const p = clamp(msg.pos, 0, mirror.value.length)
        const e = clamp(p + msg.length, 0, mirror.value.length)
        mirror.value = mirror.value.slice(0, p) + mirror.value.slice(e)
      }
      version.value = msg.version

      const isOwn =
        pending.value.length &&
        pending.value[0].opId === msg.opId &&
        pending.value[0].clientId === msg.clientId

      if (isOwn) {
        console.log('[OT] ✅ Ack for own op', msg.opId)
        pending.value.shift()
      } else {
        console.log('[OT] Transform pending against concurrent op')
        for (let i = 0; i < pending.value.length; i++) {
          pending.value[i] = transform(pending.value[i], msg)
        }
      }

      text.value = applyAll(mirror.value, pending.value)
      lastValue = text.value
      if (textarea.value) textarea.value.value = text.value
      console.log(`[OT] 🧮 v${version.value} | pending:${pending.value.length}`)
    }

    function applyAll(base, ops) {
      let s = base
      for (const op of ops) {
        if (op.type === 'insert') {
          const p = clamp(op.pos, 0, s.length)
          s = s.slice(0, p) + op.text + s.slice(p)
        } else if (op.type === 'delete') {
          const p = clamp(op.pos, 0, s.length)
          const e = clamp(p + op.length, 0, s.length)
          s = s.slice(0, p) + s.slice(e)
        }
      }
      return s
    }

    function transform(A, B) {
      A = { ...A }
      if (A.type === 'insert' && B.type === 'insert') {
        if (A.pos > B.pos || (A.pos === B.pos && tiebreak(A, B) > 0))
          A.pos += (B.text || '').length
      } else if (A.type === 'insert' && B.type === 'delete') {
        if (A.pos > B.pos) A.pos -= Math.min(B.length, A.pos - B.pos)
      } else if (A.type === 'delete' && B.type === 'insert') {
        if (A.pos >= B.pos) A.pos += (B.text || '').length
      } else if (A.type === 'delete' && B.type === 'delete') {
        if (B.pos + B.length <= A.pos) {
          A.pos -= B.length
        } else if (B.pos < A.pos + A.length) {
          const overlapStart = Math.max(A.pos, B.pos)
          const overlapEnd = Math.min(A.pos + A.length, B.pos + B.length)
          const overlap = Math.max(0, overlapEnd - overlapStart)
          A.length -= overlap
          if (B.pos < A.pos) A.pos = B.pos
        }
      }
      console.log('[OT] ✏️ Transform ->', A)
      return A
    }

    const tiebreak = (a, b) => {
      const A = `${a.clientId || ''}:${a.opId || ''}`
      const B = `${b.clientId || ''}:${b.opId || ''}`
      return A.localeCompare(B)
    }

    function onInput(e) {
      const newVal = e.target.value
      const diff = singleDiff(lastValue, newVal)
      if (!diff) return

      const { type, pos, text: insText, length } = diff
      const op =
        type === 'insert'
          ? { type: 'insert', pos, text: insText, baseVersion: version.value, clientId, opId: crypto.randomUUID() }
          : { type: 'delete', pos, length, baseVersion: version.value, clientId, opId: crypto.randomUUID() }

      console.log('[OT] ✍️ Local edit ->', op)
      pending.value.push(op)
      text.value = applyAll(mirror.value, pending.value)
      lastValue = text.value
      send({ action: 'op', docId: 'demo', op })
    }

    function singleDiff(a, b) {
      if (a === b) return null
      const minLen = Math.min(a.length, b.length)
      let start = 0
      while (start < minLen && a[start] === b[start]) start++
      let endA = a.length - 1
      let endB = b.length - 1
      // Add comment so block is not empty (fixes ESLint)
      while (endA >= start && endB >= start && a[endA] === b[endB]) {
        // decrement indexes to find diff end
        endA--
        endB--
      }
      if (a.length < b.length) {
        return { type: 'insert', pos: start, text: b.slice(start, endB + 1) }
      } else {
        return { type: 'delete', pos: start, length: endA - start + 1 }
      }
    }

    onMounted(connect)
    onBeforeUnmount(() => {
      console.log('[OT] Closing WebSocket')
      try {
        ws.value?.close()
      } catch (err) {
        // no-op: safe close
      }
      if (reconnectTimer) {
        clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
    })

    return { text, onInput, textarea, wsState, pending, version }
  }
}
</script>


<template>
  <div class="flex flex-row h-screen w-screen bg-surface-light overflow-hidden">
    <SidebarBase />
    <div class="flex flex-col h-full w-full overflow-hidden">
      <div class="flex flex-row h-[50px] w-full border-b-2 border-accent flex-shrink-0"></div>
      <div class="flex flex-col flex-1 w-full justify-center items-center overflow-hidden">
        <div class="flex flex-row h-[50px] w-[1200px] border-b border-border-light-subtle flex-shrink-0"></div>
        <div class="flex flex-row flex-1">
          <div class="flex-1 w-[1000px] bg-white border-x border-border-light-subtle px-12 overflow-hidden">
            <textarea
              ref="textarea"
              :value="text"
              @input="onInput"
              name="document-content"
              id="document-editor"
              placeholder="Start writing your document..."
              class="w-full h-full resize-none border-none focus:outline-none bg-transparent text-gray-800 text-base font-normal placeholder-gray-400 py-12"
            ></textarea>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
