import { Extension } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'

export const CollaborationCursor = Extension.create({
  name: 'collaborationCursor',

  addOptions() {
    return {
      provider: null,
      user: {
        name: 'Anonymous',
        color: '#3b82f6',
      },
      render: (user) => {
        const cursor = document.createElement('span')
        cursor.classList.add('collaboration-cursor')
        cursor.style.position = 'absolute'
        cursor.style.borderLeft = `2px solid ${user.color || '#3b82f6'}`
        cursor.style.height = '1.2em'
        cursor.style.width = '0'
        cursor.style.pointerEvents = 'none'
        cursor.style.zIndex = '100'

        const label = document.createElement('div')
        label.classList.add('collaboration-cursor__label')
        label.style.backgroundColor = user.color || '#3b82f6'
        label.style.color = '#fff'
        label.style.fontSize = '11px'
        label.style.fontWeight = '500'
        label.style.padding = '1px 4px'
        label.style.borderRadius = '2px'
        label.style.position = 'absolute'
        label.style.top = '-1.5em'
        label.style.left = '0'
        label.style.whiteSpace = 'nowrap'
        label.style.pointerEvents = 'none'
        label.style.userSelect = 'none'
        label.style.zIndex = '1000'
        label.textContent = user.name

        cursor.appendChild(label)

        return cursor
      },
    }
  },

  addProseMirrorPlugins() {
    const extension = this

    return [
      new Plugin({
        key: new PluginKey('collaborationCursor'),
        state: {
          init() {
            return {
              cursors: new Map(),
              decorations: DecorationSet.empty,
            }
          },
          apply(tr, value, oldState, newState) {
            const meta = tr.getMeta('collaborationCursor')
            
            if (meta) {
              const { cursors } = meta
              const decorations = []
              const docSize = newState.doc.content.size

              cursors.forEach((cursor, userId) => {
                if (cursor && cursor.from !== undefined && cursor.to !== undefined) {
                  // Ensure cursor positions are within valid document bounds
                  const from = Math.max(0, Math.min(cursor.from, docSize))
                  const to = Math.max(0, Math.min(cursor.to, docSize))
                  
                  // Only create decorations if positions are valid
                  if (from <= docSize && from >= 0) {
                    const decoration = Decoration.widget(from, () => {
                      return extension.options.render({
                        name: cursor.userId || userId || 'Anonymous',
                        color: cursor.color || getUserColor(userId),
                        userId: cursor.userId || userId,
                      })
                    })
                    decorations.push(decoration)
                  }

                  // Only create selection decoration if there's a valid range
                  if (from !== to && to <= docSize && to >= from) {
                    const selectionDecoration = Decoration.inline(from, to, {
                      class: 'collaboration-cursor__selection',
                      style: `background-color: ${cursor.color || getUserColor(userId)}20;`,
                    })
                    decorations.push(selectionDecoration)
                  }
                }
              })

              return {
                cursors,
                decorations: DecorationSet.create(newState.doc, decorations),
              }
            }

            if (tr.docChanged) {
              return {
                cursors: value.cursors,
                decorations: value.decorations.map(tr.mapping, tr.doc),
              }
            }

            return value
          },
        },
        props: {
          decorations(state) {
            return this.getState(state)?.decorations
          },
        },
      }),
    ]
  },
})

// Generate consistent color for user ID
function getUserColor(userId) {
  const colors = [
    '#3b82f6', // blue
    '#10b981', // green
    '#f59e0b', // amber
    '#ef4444', // red
    '#8b5cf6', // violet
    '#ec4899', // pink
    '#14b8a6', // teal
    '#f97316', // orange
  ]
  
  let hash = 0
  for (let i = 0; i < userId.length; i++) {
    hash = userId.charCodeAt(i) + ((hash << 5) - hash)
  }
  
  return colors[Math.abs(hash) % colors.length]
}
