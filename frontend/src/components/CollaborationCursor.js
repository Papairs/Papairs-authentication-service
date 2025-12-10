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
        cursor.style.borderLeft = `2px solid ${user.color || '#3b82f6'}`
        cursor.style.marginLeft = '-1px'
        cursor.style.marginRight = '-1px'
        cursor.style.pointerEvents = 'none'

        const label = document.createElement('div')
        label.classList.add('collaboration-cursor__label')
        label.style.backgroundColor = user.color || '#3b82f6'
        label.style.color = '#fff'
        label.style.fontSize = '12px'
        label.style.fontWeight = '600'
        label.style.padding = '2px 6px'
        label.style.borderRadius = '3px'
        label.style.position = 'absolute'
        label.style.top = '-1.4em'
        label.style.left = '-1px'
        label.style.whiteSpace = 'nowrap'
        label.style.pointerEvents = 'none'
        label.style.userSelect = 'none'
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

              cursors.forEach((cursor, userId) => {
                if (cursor && cursor.from !== undefined && cursor.to !== undefined) {
                  const from = Math.max(0, Math.min(cursor.from, newState.doc.content.size))
                  const to = Math.max(0, Math.min(cursor.to, newState.doc.content.size))
                  
                  if (from <= newState.doc.content.size) {
                    const decoration = Decoration.widget(from, () => {
                      return extension.options.render({
                        name: cursor.userId || userId || 'Anonymous',
                        color: cursor.color || getUserColor(userId),
                        userId: cursor.userId || userId,
                      })
                    })
                    decorations.push(decoration)
                  }

                  if (from !== to && to <= newState.doc.content.size) {
                    const selectionDecoration = Decoration.inline(from, to, {
                      class: 'collaboration-cursor__selection',
                      style: `background-color: ${cursor.color || getUserColor(userId)}33;`,
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
