<script>
import axios from 'axios'
import SidebarBase from '@/components/SidebarBase.vue'

export default {
  name: 'DocsView',
  components: {
    SidebarBase
  },
  data() {
    return {
      documents: [],
      loading: false
    }
  },
  async mounted() {
    await this.loadDocuments();
  },
  methods: {
    async loadDocuments() {
      this.loading = true;
      try {
        const response = await axios.get('http://localhost:8082/api/docs/all');
        this.documents = response.data;
      } catch (error) {
        console.error('Error loading documents:', error);
        this.documents = [];
      } finally {
        this.loading = false;
      }
    }
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
              name="document-content" 
              id="document-editor" 
              placeholder="Start writing your document..." 
              class="w-full h-full resize-none border-none focus:outline-none bg-transparent text-gray-800 text-base font-normal placeholder-gray-400 py-12"></textarea>
          </div>
        </div>
        

      </div>
    </div>
  </div>
</template>